import React from "react";
import "./App.css";

const services=[
  ["API Gateway",8080],
  ["Core Processing",8081],
  ["Fraud Risk Engine",8083],
  ["Tokenization Vault",8084],
  ["Card Management",8085]
];

const features=[
  ["payments","💳","Payment Processing","Authorization flow, balance and limit checks through the core processing service."],
  ["fraud","🛡️","Fraud Risk","Explainable rule-based scoring with ALLOW, REVIEW and BLOCK decisions."],
  ["vault","🔐","Tokenization Vault","Synthetic PANs are replaced with opaque tokens and encrypted at rest."],
  ["cards","💼","Card Management","Issue and manage the lifecycle of demo cards through a dedicated service."]
];

export default function App(){
  const [status,setStatus]=React.useState({});

  React.useEffect(()=>{
    let active=true;
    const check=async()=>{
      const next={};
      await Promise.all(services.map(async([name,port])=>{
        try{next[name]=(await fetch("/health/"+port)).ok}catch(e){next[name]=false}
      }));
      if(active)setStatus(next);
    };
    check();
    const id=setInterval(check,10000);
    return()=>{active=false;clearInterval(id)};
  },[]);

  return <main className="platform">
    <nav className="nav">
      <a href="#home" className="brand">ENTERPRISE PAYMENTS</a>
      <div>{features.map(([id,,title])=><a key={id} href={"#"+id}>{title}</a>)}</div>
    </nav>

    <section id="home" className="hero">
      <p className="eyebrow">PHASE 1 • LIVE FULL-STACK DEMO</p>
      <h1>Enterprise Payment Platform</h1>
      <p className="subtitle">One public application backed by separate Spring Boot services and PostgreSQL.</p>
      <div className="heroActions">
        <a className="primary" href="#services">View Live Services</a>
        <a className="secondary" href="#payments">Explore Features</a>
      </div>
    </section>

    <section className="featureGrid">
      {features.map(([id,icon,title,description])=>
        <a href={"#"+id} className="feature" key={id}>
          <span>{icon}</span><h2>{title}</h2><p>{description}</p>
        </a>
      )}
    </section>

    <section className="architecture">
      <h2>Phase 1 Architecture</h2>
      <code>Browser → Frontend → API Gateway → Separate Services → PostgreSQL</code>
      <p>Core Processing, Fraud Risk, Tokenization Vault and Card Management remain independently deployable services.</p>
    </section>

    <section id="services">
      <div className="sectionHead"><h2>Live Service Health</h2><span>refreshes every 10 seconds</span></div>
      <div className="grid">
        {services.map(([name,port])=><article className="service" key={name}>
          <span className={"dot "+(status[name]?"online":"offline")}></span>
          <div><h2>{name}</h2><p>{name==="API Gateway"?"single entry point":"separate backend service"}</p></div>
          <b>{status[name]?"ONLINE":"STARTING / OFFLINE"}</b>
        </article>)}
      </div>
    </section>

    {features.map(([id,icon,title,description])=>
      <section id={id} className="featureSection" key={id}>
        <span className="bigIcon">{icon}</span>
        <div><h2>{title}</h2><p>{description}</p><p className="muted">This Phase 1 deployment uses synthetic/demo data only.</p></div>
        <a className="secondary" href="#services">Check Service</a>
      </section>
    )}

    <section className="quick">
      <h2>Local development</h2>
      <code>docker compose up --build</code>
      <p>Production-style deployment configuration is defined in <strong>render.yaml</strong>.</p>
    </section>
  </main>
}