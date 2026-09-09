import React,{useState} from "react";
import "./App.css";

const nav=["Dashboard","Process Payment","Payment Switch","Fraud Risk","Core Processing"];

export default function App(){
  const [active,setActive]=useState("Dashboard");
  const [form,setForm]=useState({amount:"2500",merchant:"Demo Store",card:"4111111111111111"});
  const [result,setResult]=useState(null);
  const [loading,setLoading]=useState(false);
  const [history,setHistory]=useState([]);

  const processPayment=async(e)=>{
    e?.preventDefault();
    setLoading(true); setResult(null);
    await new Promise(r=>setTimeout(r,700));
    const amount=Number(form.amount)||0;
    const suspicious=amount>50000 || form.card.endsWith("0000");
    const risk=suspicious?82:Math.min(32,Math.max(8,Math.round(amount/250)));
    const approved=risk<75;
    const tx={id:"TXN-"+Date.now().toString().slice(-8),amount,merchant:form.merchant||"Unknown Merchant",risk,status:approved?"APPROVED":"DECLINED",time:new Date().toLocaleTimeString()};
    setResult(tx); setHistory(h=>[tx,...h].slice(0,6)); setLoading(false); setActive("Dashboard");
  };

  return <div className="app">
    <aside className="sidebar">
      <div className="brand"><span className="logo">EP</span><div><b>Enterprise</b><small>Payment Platform</small></div></div>
      <nav>{nav.map(n=><button key={n} className={active===n?"active":""} onClick={()=>setActive(n)}>{n}</button>)}</nav>
      <div className="live"><span></span> LIVE DEMO</div>
    </aside>
    <main>
      <header><div><p className="eyebrow">ENTERPRISE PAYMENT OPERATIONS</p><h1>{active}</h1></div><div className="badge">● Platform Online</div></header>

      {active==="Dashboard" && <><section className="hero">
        <div><h2>Payment operations, in one place.</h2><p>Run a transaction through the Payment Switch, Fraud Risk Engine and Core Processing flow.</p><button className="primary" onClick={()=>setActive("Process Payment")}>Process a Payment →</button></div>
        <div className="flow"><b>Transaction Flow</b><p>Client → Switch → Fraud → Core</p><div><span>Switch</span><i>→</i><span>Fraud</span><i>→</i><span>Core</span></div></div>
      </section>
      <section className="stats">
        <article><small>Transactions</small><strong>{history.length}</strong></article>
        <article><small>Approved</small><strong>{history.filter(x=>x.status==="APPROVED").length}</strong></article>
        <article><small>Fraud Checks</small><strong>{history.length}</strong></article>
        <article><small>Platform</small><strong className="ok">ONLINE</strong></article>
      </section>
      <section className="panel"><h2>Recent Transactions</h2>{history.length===0?<p className="muted">No transactions yet. Start with “Process a Payment”.</p>:<div className="table">{history.map(x=><div className="row" key={x.id}><span>{x.id}</span><span>{x.merchant}</span><span>₹{x.amount.toLocaleString()}</span><span className={x.status==="APPROVED"?"approved":"declined"}>{x.status}</span></div>)}</div>}</section></>}

      {active==="Process Payment" && <section className="panel form"><h2>Create Transaction</h2><p className="muted">Run the demo transaction through the platform flow.</p><form onSubmit={processPayment}>
        <label>Amount (₹)<input type="number" value={form.amount} onChange={e=>setForm({...form,amount:e.target.value})}/></label>
        <label>Merchant<input value={form.merchant} onChange={e=>setForm({...form,merchant:e.target.value})}/></label>
        <label>Card Number<input value={form.card} onChange={e=>setForm({...form,card:e.target.value})}/></label>
        <button className="primary" disabled={loading}>{loading?"Processing...":"Process Transaction"}</button>
      </form>
      {result&&<Result tx={result}/>}</section>}

      {active==="Payment Switch" && <Feature title="Payment Switch" text="Receives the transaction and routes it through the fraud decision before forwarding approved traffic to core processing." status="Ready for live backend connection" steps={["Receive transaction","Validate request","Request fraud decision","Route approved transaction"]}/>}
      {active==="Fraud Risk" && <Feature title="Fraud Risk Engine" text="Evaluates transaction risk before authorization. High-risk transactions are blocked before they reach the core processor." status="Synchronous risk decision flow" steps={["Analyze amount and signals","Calculate risk score","Approve or block","Return decision to switch"]}/>}
      {active==="Core Processing" && <Feature title="Core Processing Engine" text="Handles the final business processing step after the Payment Switch receives a safe fraud decision." status="Processing layer ready" steps={["Receive routed transaction","Apply business rules","Create processing decision","Return final status"]}/>}
    </main>
  </div>
}
function Result({tx}){return <div className="result"><h3>{tx.status==="APPROVED"?"✓ Transaction Approved":"⚠ Transaction Declined"}</h3><p><b>{tx.id}</b> · {tx.merchant} · ₹{tx.amount.toLocaleString()}</p><div className="risk">Fraud Risk Score: <b>{tx.risk}/100</b></div><p className="muted">Payment Switch → Fraud Risk Engine → Core Processing</p></div>}
function Feature({title,text,status,steps}){return <><section className="hero compact"><div><h2>{title}</h2><p>{text}</p><div className="badge">{status}</div></div></section><section className="panel"><h2>Processing Flow</h2>{steps.map((s,i)=><div className="step" key={s}><b>{i+1}</b><span>{s}</span></div>)}</section></>}