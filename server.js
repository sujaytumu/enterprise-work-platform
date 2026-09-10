const express=require("express");
const path=require("path");
const {Pool}=require("pg");

if(!process.env.DATABASE_URL){
  console.error("DATABASE_URL is required. This deployment uses a real PostgreSQL database.");
  process.exit(1);
}

const pool=new Pool({
  connectionString:process.env.DATABASE_URL,
  ssl: process.env.DATABASE_URL.includes("localhost") ? false : undefined
});
const app=express();
app.use(express.json());

const riskFor=(amount,merchant,card)=>{
  let score=8;
  const factors=[];
  if(amount>=50000){score+=42;factors.push("High transaction amount");}
  if(amount>=25000&&amount<50000){score+=20;factors.push("Elevated transaction amount");}
  if(/casino|crypto|unknown|test/i.test(merchant||"")){score+=28;factors.push("High-risk merchant pattern");}
  if(String(card||"").endsWith("0000")){score+=25;factors.push("Suspicious card pattern");}
  if(!factors.length) factors.push("Normal transaction profile");
  return {score:Math.min(score,99),factors};
};

async function init(){
  await pool.query(`
    CREATE TABLE IF NOT EXISTS transactions(
      id BIGSERIAL PRIMARY KEY,
      transaction_ref VARCHAR(40) UNIQUE NOT NULL,
      amount NUMERIC(14,2) NOT NULL,
      merchant VARCHAR(120) NOT NULL,
      card_last4 VARCHAR(4) NOT NULL,
      token VARCHAR(80) NOT NULL,
      risk_score INTEGER NOT NULL,
      fraud_decision VARCHAR(20) NOT NULL,
      core_decision VARCHAR(20) NOT NULL,
      status VARCHAR(20) NOT NULL,
      iso_code VARCHAR(4) NOT NULL,
      created_at TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS fraud_assessments(
      id BIGSERIAL PRIMARY KEY,
      transaction_ref VARCHAR(40) UNIQUE NOT NULL,
      risk_score INTEGER NOT NULL,
      decision VARCHAR(20) NOT NULL,
      factors JSONB NOT NULL,
      created_at TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE TABLE IF NOT EXISTS settlement_batches(
      id BIGSERIAL PRIMARY KEY,
      batch_ref VARCHAR(40) UNIQUE NOT NULL,
      transaction_count INTEGER NOT NULL,
      gross_amount NUMERIC(14,2) NOT NULL,
      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
      created_at TIMESTAMPTZ DEFAULT NOW()
    );
  `);
}

app.get("/api/health",async(req,res)=>{
  try{await pool.query("SELECT 1");res.json({status:"UP",database:"POSTGRESQL",modules:7});}
  catch(e){res.status(503).json({status:"DOWN"});}
});

app.get("/api/dashboard",async(req,res)=>{
  const q=await pool.query(`
    SELECT COUNT(*)::int total,
    COUNT(*) FILTER (WHERE status='APPROVED')::int approved,
    COUNT(*) FILTER (WHERE status='DECLINED')::int declined,
    COUNT(*) FILTER (WHERE fraud_decision='BLOCK')::int blocked,
    COALESCE(SUM(amount) FILTER (WHERE status='APPROVED'),0)::numeric volume
    FROM transactions`);
  res.json(q.rows[0]);
});

app.get("/api/transactions",async(req,res)=>{
  const q=await pool.query("SELECT transaction_ref,amount,merchant,card_last4,risk_score,fraud_decision,core_decision,status,iso_code,created_at FROM transactions ORDER BY created_at DESC LIMIT 30");
  res.json(q.rows);
});

app.get("/api/transactions/:ref",async(req,res)=>{
  const q=await pool.query(`
    SELECT t.*,f.factors FROM transactions t
    LEFT JOIN fraud_assessments f ON f.transaction_ref=t.transaction_ref
    WHERE t.transaction_ref=$1`,[req.params.ref]);
  if(!q.rowCount)return res.status(404).json({message:"Transaction not found"});
  res.json(q.rows[0]);
});

app.post("/api/transactions",async(req,res)=>{
  const {amount,merchant,cardNumber}=req.body||{};
  const value=Number(amount);
  if(!Number.isFinite(value)||value<=0||value>1000000)return res.status(400).json({message:"Enter a valid amount"});
  if(!merchant||String(merchant).trim().length<2)return res.status(400).json({message:"Merchant is required"});
  const digits=String(cardNumber||"").replace(/\D/g,"");
  if(digits.length<12||digits.length>19)return res.status(400).json({message:"Use a valid demo card number"});
  const ref="TXN-"+Date.now().toString(36).toUpperCase()+"-"+Math.random().toString(36).slice(2,6).toUpperCase();
  const token="tok_"+Buffer.from(digits).toString("base64url").slice(0,24);
  const risk=riskFor(value,merchant,digits);
  const blocked=risk.score>=75;
  const status=blocked?"DECLINED":"APPROVED";
  const fraudDecision=blocked?"BLOCK":"ALLOW";
  const coreDecision=blocked?"NOT_ROUTED":"AUTHORIZED";
  const isoCode=blocked?"05":"00";
  const client=await pool.connect();
  try{
    await client.query("BEGIN");
    await client.query(`INSERT INTO transactions(transaction_ref,amount,merchant,card_last4,token,risk_score,fraud_decision,core_decision,status,iso_code)
      VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)`,
      [ref,value,String(merchant).trim(),digits.slice(-4),token,risk.score,fraudDecision,coreDecision,status,isoCode]);
    await client.query("INSERT INTO fraud_assessments(transaction_ref,risk_score,decision,factors) VALUES($1,$2,$3,$4::jsonb)",
      [ref,risk.score,fraudDecision,JSON.stringify(risk.factors)]);
    await client.query("COMMIT");
    res.status(201).json({
      transactionRef:ref,amount:value,merchant:String(merchant).trim(),
      cardLast4:digits.slice(-4),token,riskScore:risk.score,factors:risk.factors,
      fraudDecision,coreDecision,status,isoCode,
      flow:["Tokenization Vault","Payment Switch","Fraud Risk Engine",...(blocked?[]:["Core Processing Engine"]),"PostgreSQL Persistence"]
    });
  }catch(e){await client.query("ROLLBACK");console.error(e);res.status(500).json({message:"Transaction could not be persisted"});}
  finally{client.release();}
});

app.post("/api/settlement/run",async(req,res)=>{
  const q=await pool.query("SELECT COUNT(*)::int c,COALESCE(SUM(amount),0)::numeric a FROM transactions WHERE status='APPROVED' AND created_at>=CURRENT_DATE");
  const ref="SET-"+Date.now().toString(36).toUpperCase();
  const b=await pool.query("INSERT INTO settlement_batches(batch_ref,transaction_count,gross_amount,status) VALUES($1,$2,$3,'COMPLETED') RETURNING *",[ref,q.rows[0].c,q.rows[0].a]);
  res.json(b.rows[0]);
});

app.get("/api/modules",async(req,res)=>res.json([
 {name:"Card Management",status:"READY",description:"Card lifecycle and account controls"},
 {name:"Tokenization Vault",status:"ACTIVE",description:"Transforms demo PAN into a platform token"},
 {name:"Payment Switch",status:"ACTIVE",description:"Validates and routes transaction requests"},
 {name:"Fraud Risk Engine",status:"ACTIVE",description:"Rule-based synchronous risk scoring"},
 {name:"Core Processing",status:"ACTIVE",description:"Authorization and final processing decision"},
 {name:"Clearing & Settlement",status:"READY",description:"Groups approved transactions into settlement batches"},
 {name:"Task Management",status:"READY",description:"Operational workflow foundation"}
]));

app.use(express.static(path.join(__dirname,"client","build")));
app.get("*",(req,res)=>res.sendFile(path.join(__dirname,"client","build","index.html")));

init().then(()=>{
 const port=process.env.PORT||10000;
 app.listen(port,"0.0.0.0",()=>console.log("Enterprise platform listening on "+port));
}).catch(e=>{console.error("Database initialization failed",e);process.exit(1);});
