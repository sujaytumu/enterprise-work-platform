const express=require("express");
const path=require("path");
const crypto=require("crypto");
const {Pool}=require("pg");

const app=express();
app.use(express.json({limit:"32kb"}));

if(!process.env.DATABASE_URL){
  console.error("DATABASE_URL is missing. A real PostgreSQL connection is required.");
  process.exit(1);
}

const pool=new Pool({
  connectionString:process.env.DATABASE_URL,
  max:10,
  idleTimeoutMillis:30000,
  connectionTimeoutMillis:10000,
  ssl:process.env.DATABASE_URL.includes("localhost")?false:undefined
});

const ref=(prefix)=>prefix+"-"+Date.now().toString(36).toUpperCase()+"-"+crypto.randomBytes(3).toString("hex").toUpperCase();
const createToken=()=> "tok_"+crypto.randomBytes(24).toString("base64url");

function assessRisk(amount,merchant,card){
  let score=8;
  const factors=[];
  const normalizedMerchant=String(merchant||"").toLowerCase();

  if(amount>=100000){score+=50;factors.push("Very high transaction amount");}
  else if(amount>=50000){score+=38;factors.push("High transaction amount");}
  else if(amount>=25000){score+=18;factors.push("Elevated transaction amount");}

  if(/casino|crypto|unknown|test/i.test(normalizedMerchant)){
    score+=28; factors.push("High-risk merchant category");
  }
  if(String(card||"").endsWith("0000")){
    score+=25; factors.push("Suspicious demo card pattern");
  }
  if(!factors.length) factors.push("Normal transaction profile");

  return {score:Math.min(score,99),factors};
}

async function initDatabase(){
  await pool.query(`
    CREATE TABLE IF NOT EXISTS transactions(
      id BIGSERIAL PRIMARY KEY,
      transaction_ref VARCHAR(40) UNIQUE NOT NULL,
      amount NUMERIC(14,2) NOT NULL CHECK(amount>0),
      merchant VARCHAR(120) NOT NULL,
      card_last4 VARCHAR(4) NOT NULL,
      token VARCHAR(80) UNIQUE NOT NULL,
      risk_score INTEGER NOT NULL CHECK(risk_score BETWEEN 0 AND 100),
      fraud_decision VARCHAR(20) NOT NULL,
      core_decision VARCHAR(20) NOT NULL,
      status VARCHAR(20) NOT NULL,
      iso_code VARCHAR(4) NOT NULL,
      settled BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS fraud_assessments(
      id BIGSERIAL PRIMARY KEY,
      transaction_ref VARCHAR(40) UNIQUE NOT NULL REFERENCES transactions(transaction_ref) ON DELETE CASCADE,
      risk_score INTEGER NOT NULL,
      decision VARCHAR(20) NOT NULL,
      factors JSONB NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS settlement_batches(
      id BIGSERIAL PRIMARY KEY,
      batch_ref VARCHAR(40) UNIQUE NOT NULL,
      transaction_count INTEGER NOT NULL DEFAULT 0,
      gross_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
      status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
    CREATE INDEX IF NOT EXISTS idx_transactions_settlement ON transactions(status,settled);
  `);
}

app.get("/api/health",async(req,res)=>{
  try{
    const q=await pool.query("SELECT current_database() AS database, NOW() AS checked_at");
    res.json({status:"UP",database:"POSTGRESQL",databaseName:q.rows[0].database,modules:7,checkedAt:q.rows[0].checked_at});
  }catch(error){
    console.error("Health check failed",error.message);
    res.status(503).json({status:"DOWN",database:"POSTGRESQL"});
  }
});

app.get("/api/dashboard",async(req,res)=>{
  const q=await pool.query(`
    SELECT
      COUNT(*)::int AS total,
      COUNT(*) FILTER (WHERE status='APPROVED')::int AS approved,
      COUNT(*) FILTER (WHERE status='DECLINED')::int AS declined,
      COUNT(*) FILTER (WHERE fraud_decision='BLOCK')::int AS blocked,
      COALESCE(SUM(amount) FILTER (WHERE status='APPROVED'),0)::numeric AS volume
    FROM transactions
  `);
  res.json(q.rows[0]);
});

app.get("/api/transactions",async(req,res)=>{
  const q=await pool.query(`
    SELECT transaction_ref,amount,merchant,card_last4,risk_score,
           fraud_decision,core_decision,status,iso_code,settled,created_at
    FROM transactions ORDER BY created_at DESC LIMIT 50
  `);
  res.json(q.rows);
});

app.get("/api/transactions/:ref",async(req,res)=>{
  const q=await pool.query(`
    SELECT t.transaction_ref,t.amount,t.merchant,t.card_last4,t.token,t.risk_score,
           t.fraud_decision,t.core_decision,t.status,t.iso_code,t.settled,t.created_at,
           f.factors
    FROM transactions t
    LEFT JOIN fraud_assessments f ON f.transaction_ref=t.transaction_ref
    WHERE t.transaction_ref=$1
  `,[req.params.ref]);

  if(!q.rowCount) return res.status(404).json({message:"Transaction not found"});
  res.json(q.rows[0]);
});

app.post("/api/transactions",async(req,res)=>{
  const {amount,merchant,cardNumber}=req.body||{};
  const value=Number(amount);
  const merchantName=String(merchant||"").trim();
  const digits=String(cardNumber||"").replace(/\D/g,"");

  if(!Number.isFinite(value)||value<=0||value>1000000)
    return res.status(400).json({message:"Enter an amount between ₹1 and ₹10,00,000"});
  if(merchantName.length<2||merchantName.length>120)
    return res.status(400).json({message:"Merchant must contain 2 to 120 characters"});
  if(digits.length<12||digits.length>19)
    return res.status(400).json({message:"Use a valid synthetic demo card number"});

  const transactionRef=ref("TXN");
  const token=createToken();
  const risk=assessRisk(value,merchantName,digits);
  const blocked=risk.score>=75;
  const status=blocked?"DECLINED":"APPROVED";
  const fraudDecision=blocked?"BLOCK":"ALLOW";
  const coreDecision=blocked?"NOT_ROUTED":"AUTHORIZED";
  const isoCode=blocked?"05":"00";

  const client=await pool.connect();
  try{
    await client.query("BEGIN");
    await client.query(`
      INSERT INTO transactions(
        transaction_ref,amount,merchant,card_last4,token,risk_score,
        fraud_decision,core_decision,status,iso_code
      ) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)
    `,[
      transactionRef,value,merchantName,digits.slice(-4),token,risk.score,
      fraudDecision,coreDecision,status,isoCode
    ]);

    await client.query(`
      INSERT INTO fraud_assessments(transaction_ref,risk_score,decision,factors)
      VALUES($1,$2,$3,$4::jsonb)
    `,[transactionRef,risk.score,fraudDecision,JSON.stringify(risk.factors)]);

    await client.query("COMMIT");

    res.status(201).json({
      transactionRef,amount:value,merchant:merchantName,cardLast4:digits.slice(-4),
      token,riskScore:risk.score,factors:risk.factors,
      fraudDecision,coreDecision,status,isoCode,
      flow:[
        "Tokenization Vault",
        "Payment Switch",
        "Fraud Risk Engine",
        ...(blocked?[]:["Core Processing"]),
        "PostgreSQL"
      ]
    });
  }catch(error){
    await client.query("ROLLBACK");
    console.error("Transaction persistence failed",error);
    res.status(500).json({message:"Transaction could not be persisted"});
  }finally{
    client.release();
  }
});

app.post("/api/settlement/run",async(req,res)=>{
  const client=await pool.connect();
  try{
    await client.query("BEGIN");
    const q=await client.query(`
      SELECT COUNT(*)::int AS count,COALESCE(SUM(amount),0)::numeric AS amount
      FROM transactions
      WHERE status='APPROVED' AND settled=FALSE
    `);

    const batchRef=ref("SET");
    const batch=await client.query(`
      INSERT INTO settlement_batches(batch_ref,transaction_count,gross_amount,status)
      VALUES($1,$2,$3,'COMPLETED') RETURNING *
    `,[batchRef,q.rows[0].count,q.rows[0].amount]);

    await client.query("UPDATE transactions SET settled=TRUE WHERE status='APPROVED' AND settled=FALSE");
    await client.query("COMMIT");
    res.json(batch.rows[0]);
  }catch(error){
    await client.query("ROLLBACK");
    console.error("Settlement failed",error);
    res.status(500).json({message:"Settlement batch could not be created"});
  }finally{
    client.release();
  }
});

app.get("/api/modules",(req,res)=>res.json([
  {name:"Card Management",status:"READY",description:"Card lifecycle and account controls"},
  {name:"Tokenization Vault",status:"ACTIVE",description:"Generates non-reversible transaction tokens"},
  {name:"Payment Switch",status:"ACTIVE",description:"Validates and routes transaction requests"},
  {name:"Fraud Risk Engine",status:"ACTIVE",description:"Rule-based synchronous risk scoring"},
  {name:"Core Processing",status:"ACTIVE",description:"Authorization and final processing decision"},
  {name:"Clearing & Settlement",status:"ACTIVE",description:"Settles approved transactions once per batch"},
  {name:"Task Management",status:"READY",description:"Operational workflow foundation"}
]));

app.use(express.static(path.join(__dirname,"client","build")));
app.get("*",(req,res)=>res.sendFile(path.join(__dirname,"client","build","index.html")));

app.use((error,req,res,next)=>{
  console.error("Unhandled request error",error);
  res.status(500).json({message:"Unexpected platform error"});
});

initDatabase()
  .then(()=>{
    const port=Number(process.env.PORT)||10000;
    app.listen(port,"0.0.0.0",()=>console.log("Enterprise platform listening on port "+port));
  })
  .catch(error=>{
    console.error("Database initialization failed",error);
    process.exit(1);
  });
