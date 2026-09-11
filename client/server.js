const express=require("express");const path=require("path");const crypto=require("crypto");
const app=express();app.use(express.json());app.use(express.static(path.join(__dirname,"build")));
let txs=[],batches=[];
const modules=[
{name:"API Gateway",description:"Single public entry and API routing",status:"ACTIVE"},
{name:"Core Processing",description:"Final authorization decision",status:"ACTIVE"},
{name:"Payment Switch",description:"Transaction validation and routing",status:"ACTIVE"},
{name:"Fraud Risk Engine",description:"Risk scoring and fraud blocking",status:"ACTIVE"},
{name:"Tokenization Vault",description:"Masks demo card data into a token",status:"ACTIVE"},
{name:"Card Management",description:"Card lifecycle capability retained in architecture",status:"PLANNED"},
{name:"Clearing & Settlement",description:"Batch settlement of approved transactions",status:"ACTIVE"}];
const score=(amount,card,merchant)=>{let s=8,f=[];if(amount>=50000){s+=55;f.push("High transaction amount");}if(String(card).endsWith("0000")){s+=30;f.push("Suspicious demo card pattern");}if(/cash|unknown/i.test(merchant||"")){s+=12;f.push("Merchant pattern requires review");}if(!f.length)f.push("Normal transaction pattern");return {score:Math.min(s,100),f};};
app.get("/api/health",(req,res)=>res.json({status:"UP",mode:"single-service-live"}));
app.get("/api/modules",(req,res)=>res.json(modules));
app.get("/api/dashboard",(req,res)=>{const approved=txs.filter(x=>x.status==="APPROVED");res.json({total:txs.length,approved:approved.length,blocked:txs.filter(x=>x.status==="DECLINED").length,volume:approved.reduce((a,x)=>a+Number(x.amount),0)});});
app.get("/api/transactions",(req,res)=>res.json(txs.slice().reverse()));
app.get("/api/transactions/:ref",(req,res)=>{const x=txs.find(t=>t.transaction_ref===req.params.ref);x?res.json(x):res.status(404).json({message:"Transaction not found"});});
app.post("/api/transactions",(req,res)=>{const amount=Number(req.body.amount);const merchant=String(req.body.merchant||"Demo Merchant");const card=String(req.body.cardNumber||"");if(!Number.isFinite(amount)||amount<=0)return res.status(400).json({message:"Enter a valid amount"});const r=score(amount,card,merchant),status=r.score>=75?"DECLINED":"APPROVED";const x={transaction_ref:"TXN-"+Date.now().toString().slice(-8)+"-"+crypto.randomBytes(2).toString("hex").toUpperCase(),merchant,amount,card_last4:card.slice(-4)||"0000",risk_score:r.score,status,factors:r.f,created_at:new Date().toISOString(),fraud_decision:status==="APPROVED"?"ALLOW":"BLOCK",core_decision:status==="APPROVED"?"AUTHORIZED":"NOT_AUTHORIZED"};txs.push(x);res.json({...x,isoCode:status==="APPROVED"?"00":"05",riskScore:r.score,fraudDecision:x.fraud_decision,coreDecision:x.core_decision,flow:["Tokenization Vault","Payment Switch","Fraud Risk Engine","Core Processing","PostgreSQL"]});});
app.post("/api/settlement/run",(req,res)=>{const approved=txs.filter(x=>x.status==="APPROVED");const b={batch_ref:"SET-"+Date.now().toString().slice(-8),transaction_count:approved.length,gross_amount:approved.reduce((a,x)=>a+Number(x.amount),0),status:"SETTLED",created_at:new Date().toISOString()};batches.push(b);res.json(b);});
app.get("*",(req,res)=>res.sendFile(path.join(__dirname,"build","index.html")));
app.listen(process.env.PORT||3000,()=>console.log("Enterprise platform running"));