import React,{useEffect,useState} from "react";
import "./App.css";

const nav=[
 ["Dashboard","▦"],["Process Payment","＋"],["Transactions","≡"],
 ["Payment Switch","⇄"],["Fraud Risk","◈"],["Core Processing","◎"],
 ["Tokenization","◇"],["Settlement","▤"],["Platform Modules","◌"]
];
const money=v=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR",maximumFractionDigits:0}).format(Number(v||0));

export default function App(){
 const [active,setActive]=useState("Dashboard");
 const [stats,setStats]=useState({}); const [history,setHistory]=useState([]);
 const [modules,setModules]=useState([]); const [health,setHealth]=useState(null);
 const [form,setForm]=useState({amount:"2500",merchant:"Amazon Demo",cardNumber:"4111111111111111"});
 const [result,setResult]=useState(null); const [loading,setLoading]=useState(false); const [selected,setSelected]=useState(null);

 const refresh=async()=>{try{
   const [d,t,m,h]=await Promise.all(["/api/dashboard","/api/transactions","/api/modules","/api/health"].map(x=>fetch(x).then(r=>r.json())));
   setStats(d);setHistory(t);setModules(m);setHealth(h);
 }catch(e){setHealth({status:"DOWN"});}};
 useEffect(()=>{refresh();},[]);

 const processPayment=async e=>{
   e.preventDefault();setLoading(true);setResult(null);
   try{const r=await fetch("/api/transactions",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(form)});
     const d=await r.json();if(!r.ok)throw new Error(d.message||"Unable to process transaction");
     setResult(d);await refresh();
   }catch(err){setResult({error:err.message});}finally{setLoading(false);}
 };

 const page=()=>{
  if(active==="Dashboard")return <Dashboard stats={stats} history={history} go={setActive} open={setSelected}/>;
  if(active==="Process Payment")return <Payment form={form} setForm={setForm} loading={loading} process={processPayment} result={result}/>;
  if(active==="Transactions")return <Transactions history={history} open={setSelected}/>;
  if(active==="Settlement")return <Settlement refresh={refresh}/>;
  if(active==="Platform Modules")return <Modules modules={modules}/>;
  return <ModulePage name={active} history={history}/>;
 };
 return <div className="app">
  <aside className="sidebar">
   <div className="brand"><span className="logo">EP</span><div><b>Enterprise</b><small>Payment Platform</small></div></div>
   <nav>{nav.map(([n,i])=><button key={n} className={active===n?"active":""} onClick={()=>setActive(n)}><span>{i}</span>{n}</button>)}</nav>
   <div className="sidefoot"><i className={health?.status==="UP"?"dot":"dot bad"}></i><b>{health?.status==="UP"?"SYSTEM OPERATIONAL":"CHECKING SYSTEM"}</b><small>PostgreSQL persistence enabled</small></div>
  </aside>
  <main>
   <header><div><p className="eyebrow">ENTERPRISE PAYMENT OPERATIONS</p><h1>{active}</h1></div><div className={health?.status==="UP"?"online":"online off"}>● {health?.status==="UP"?"Platform Online":"Connecting..."}</div></header>
   {page()}
  </main>
  {selected&&<TransactionModal refId={selected} close={()=>setSelected(null)}/>}
 </div>
}

function Dashboard({stats,history,go,open}){return <div>
 <section className="hero">
  <div><span className="kicker">LIVE OPERATIONS CONSOLE</span><h2>Every payment. One intelligent flow.</h2><p>Route transactions through tokenization, switching, fraud scoring and core authorization — then persist the final result in PostgreSQL.</p><button className="primary" onClick={()=>go("Process Payment")}>Process a Transaction <b>→</b></button></div>
  <div className="pipeline"><div className="pipeTitle">Transaction Pipeline <span>LIVE</span></div>{["Tokenize","Switch","Risk","Authorize","Persist"].map((x,i)=><React.Fragment key={x}><div className="node"><b>{i+1}</b><span>{x}</span></div>{i<4&&<div className="line">→</div>}</React.Fragment>)}</div>
 </section>
 <section className="stats">
  <Stat label="Total Transactions" value={stats.total||0} icon="↔"/>
  <Stat label="Approved" value={stats.approved||0} icon="✓" good/>
  <Stat label="Fraud Blocked" value={stats.blocked||0} icon="!" warn/>
  <Stat label="Approved Volume" value={money(stats.volume)} icon="₹"/>
 </section>
 <section className="panel"><div className="panelHead"><div><span className="eyebrow">REAL DATABASE RECORDS</span><h2>Recent Transactions</h2></div><button className="textbtn" onClick={()=>go("Transactions")}>View all →</button></div>
  <TxTable history={history.slice(0,7)} open={open}/>
 </section>
</div>}

function Stat({label,value,icon,good,warn}){return <article className={"stat "+(good?"good ":"")+(warn?"warn":"")}><div><small>{label}</small><strong>{value}</strong></div><span>{icon}</span></article>}

function Payment({form,setForm,loading,process,result}){return <div className="paymentGrid">
 <section className="panel formCard"><div className="panelHead"><div><span className="eyebrow">INITIATE PAYMENT</span><h2>Create Transaction</h2></div><span className="secure">DEMO DATA ONLY</span></div>
 <p className="muted">Use synthetic card data. The transaction result is processed by the backend and stored in PostgreSQL.</p>
 <form onSubmit={process}>
  <label>Transaction Amount<input type="number" min="1" value={form.amount} onChange={e=>setForm({...form,amount:e.target.value})}/></label>
  <label>Merchant<input value={form.merchant} onChange={e=>setForm({...form,merchant:e.target.value})}/></label>
  <label>Demo Card Number<input value={form.cardNumber} onChange={e=>setForm({...form,cardNumber:e.target.value})}/></label>
  <button className="primary wide" disabled={loading}>{loading?"Processing through platform...":"Process Transaction →"}</button>
 </form>
 <p className="hint">Try ₹75,000 or a card ending in <b>0000</b> to demonstrate a high-risk decision.</p>
 </section>
 <section className="flowCard"><span className="eyebrow">PROCESSING TRACE</span><h2>What happens next?</h2>
  <Trace result={result} loading={loading}/>
 </section>
 {result&&<section className={"resultCard "+(result.status==="APPROVED"?"success":"danger")+(result.error?" error":"")}>
  {result.error?<><h2>Transaction failed</h2><p>{result.error}</p></>:<><div className="resultTop"><div><span>{result.status==="APPROVED"?"✓":"!"}</span><div><small>FINAL DECISION</small><h2>{result.status}</h2></div></div><b>ISO {result.isoCode}</b></div>
  <div className="resultGrid"><p><small>REFERENCE</small>{result.transactionRef}</p><p><small>RISK SCORE</small>{result.riskScore}/100</p><p><small>FRAUD</small>{result.fraudDecision}</p><p><small>CORE</small>{result.coreDecision}</p></div>
  <div className="factorBox"><b>Risk signals</b>{result.factors?.map(x=><span key={x}>• {x}</span>)}</div></>}
 </section>}
 </div>}

function Trace({result,loading}){const steps=["Tokenization Vault","Payment Switch","Fraud Risk Engine","Core Processing","PostgreSQL"];const flow=result?.flow||steps;return <div className="trace">{steps.map((s,i)=>{const reached=result?flow.includes(s):false;return <div className={(loading?"pulse ":"")+(reached?"reached":"")} key={s}><b>{i+1}</b><span>{s}</span><small>{i===0?"Create token":i===1?"Validate & route":i===2?"Calculate risk":i===3?"Authorize decision":"Persist record"}</small></div>})}</div>}

function Transactions({history,open}){return <section className="panel"><div className="panelHead"><div><span className="eyebrow">POSTGRESQL TRANSACTION LEDGER</span><h2>Transaction History</h2></div><span className="count">{history.length} records loaded</span></div><TxTable history={history} open={open}/></section>}

function TxTable({history,open}){if(!history.length)return <div className="empty"><b>No persisted transactions yet.</b><span>Create one from Process Payment — then refresh the page to verify PostgreSQL persistence.</span></div>;return <div className="table"><div className="tr th"><span>Transaction</span><span>Merchant</span><span>Amount</span><span>Risk</span><span>Status</span></div>{history.map(x=><button className="tr" key={x.transaction_ref} onClick={()=>open(x.transaction_ref)}><span><b>{x.transaction_ref}</b><small>•••• {x.card_last4}</small></span><span>{x.merchant}</span><span>{money(x.amount)}</span><span><i className={Number(x.risk_score)>=75?"riskHigh":"riskLow"}></i>{x.risk_score}/100</span><span className={x.status==="APPROVED"?"approved":"declined"}>{x.status}</span></button>)}</div>}

function ModulePage({name,history}){const data={
 "Payment Switch":["Routes every incoming transaction through validation and risk evaluation before authorization.","Receive request","Validate transaction","Route to fraud decision","Forward approved request"],
 "Fraud Risk":["Calculates a synchronous risk score using transaction signals. High-risk transactions are blocked before core authorization.","Analyze amount","Evaluate merchant pattern","Calculate weighted risk","ALLOW or BLOCK"],
 "Core Processing":["Creates the final authorization decision for transactions that pass fraud controls.","Receive safe transaction","Apply authorization rules","Create final decision","Return ISO response"],
 "Tokenization":["Transforms synthetic card data into a platform token before processing. The stored transaction keeps only the last four digits.","Receive demo PAN","Create token","Mask card data","Pass token downstream"]
 }[name]||["Module is part of the enterprise platform.","Input","Validate","Process","Persist"];
 return <div className="moduleGrid"><section className="moduleHero"><span className="eyebrow">ACTIVE PLATFORM MODULE</span><h2>{name}</h2><p>{data[0]}</p><div className="moduleStatus">● OPERATIONAL</div></section><section className="panel"><span className="eyebrow">EXECUTION FLOW</span><h2>How this module works</h2>{data.slice(1).map((x,i)=><div className="step" key={x}><b>{i+1}</b><div><strong>{x}</strong><small>{i===3?"Produces a decision used by the next platform stage.":"Part of the backend transaction workflow."}</small></div></div>)}</section><section className="panel mini"><span className="eyebrow">LIVE CONTEXT</span><h2>{history.length} persisted transactions</h2><p className="muted">This module is represented in the single-application workflow. The complete local microservice source remains in the repository.</p></section></div>}

function Modules({modules}){return <section className="modules"><div className="moduleIntro"><span className="eyebrow">SEVEN MODULE ARCHITECTURE</span><h2>One deployment. All core capabilities.</h2><p>The public demo is a single deployable application, while the repository retains the original modular architecture for local and distributed deployment.</p></div><div className="moduleCards">{modules.map((m,i)=><article key={m.name}><span>{String(i+1).padStart(2,"0")}</span><h3>{m.name}</h3><p>{m.description}</p><b>● {m.status}</b></article>)}</div></section>}

function Settlement({refresh}){const [batch,setBatch]=useState(null);const [loading,setLoading]=useState(false);const run=async()=>{setLoading(true);const r=await fetch("/api/settlement/run",{method:"POST"});setBatch(await r.json());setLoading(false);refresh();};return <div className="settlement"><section className="moduleHero"><span className="eyebrow">CLEARING & SETTLEMENT</span><h2>Close today's approved activity.</h2><p>Create a settlement batch from real approved transactions currently stored in PostgreSQL.</p><button className="primary" onClick={run} disabled={loading}>{loading?"Creating batch...":"Run Settlement Batch →"}</button></section>{batch&&<section className="resultCard success"><h2>Settlement completed</h2><div className="resultGrid"><p><small>BATCH</small>{batch.batch_ref}</p><p><small>TRANSACTIONS</small>{batch.transaction_count}</p><p><small>GROSS AMOUNT</small>{money(batch.gross_amount)}</p><p><small>STATUS</small>{batch.status}</p></div></section>}</div>}

function TransactionModal({refId,close}){const [d,setD]=useState(null);useEffect(()=>{fetch("/api/transactions/"+refId).then(r=>r.json()).then(setD)},[refId]);return <div className="modalBack" onClick={close}><div className="modal" onClick={e=>e.stopPropagation()}><button className="close" onClick={close}>×</button>{!d?<p>Loading transaction...</p>:<><span className="eyebrow">TRANSACTION DETAIL</span><h2>{d.transaction_ref}</h2><div className="resultGrid"><p><small>MERCHANT</small>{d.merchant}</p><p><small>AMOUNT</small>{money(d.amount)}</p><p><small>RISK</small>{d.risk_score}/100</p><p><small>STATUS</small>{d.status}</p></div><div className="factorBox"><b>Persisted fraud signals</b>{(d.factors||[]).map(x=><span key={x}>• {x}</span>)}</div></>}</div></div>}
