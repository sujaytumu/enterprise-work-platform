import React, { useEffect, useState } from "react";
import "./App.css";

const services = [
  ["API Gateway", 8080],
  ["Core Processing", 8081],
  ["Payment Switch", 8082],
  ["Fraud Risk Engine", 8083],
  ["Tokenization Vault", 8084],
  ["Card Management", 8085],
  ["Clearing & Settlement", 8086]
];

export default function App() {
  const [health, setHealth] = useState({});

  useEffect(() => {
    const check = async () => {
      const results = {};
      await Promise.all(services.map(async ([name, port]) => {
        try {
          const response = await fetch("http://localhost:" + port + "/actuator/health");
          results[name] = response.ok;
        } catch {
          results[name] = false;
        }
      }));
      setHealth(results);
    };
    check();
    const timer = setInterval(check, 5000);
    return () => clearInterval(timer);
  }, []);

  return (
    <main className="app">
      <header>
        <p className="eyebrow">REFERENCE IMPLEMENTATION</p>
        <h1>Enterprise Payment Platform</h1>
        <p>Microservice-based card issuance and payment processing demo.</p>
      </header>
      <section className="grid">
        {services.map(([name, port]) => {
          const ok = health[name];
          return <article className="card" key={name}>
            <span className={"dot " + (ok ? "up" : "down")}></span>
            <div>
              <h2>{name}</h2>
              <p>localhost:{port}</p>
            </div>
            <strong>{ok ? "ONLINE" : "CHECKING / OFFLINE"}</strong>
          </article>;
        })}
      </section>
      <section className="info">
        <h2>Quick start</h2>
        <code>docker compose up --build</code>
        <p>Health checks refresh automatically every 5 seconds.</p>
      </section>
    </main>
  );
}
