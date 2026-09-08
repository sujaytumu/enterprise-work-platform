import "./App.css";

const services = [
  ["API Gateway", "8080", "/actuator/health"],
  ["Core Processing", "8081", "/actuator/health"],
  ["Payment Switch", "8082", "/actuator/health"],
  ["Fraud Risk Engine", "8083", "/actuator/health"],
  ["Tokenization Vault", "8084", "/actuator/health"],
  ["Card Management", "8085", "/actuator/health"],
  ["Clearing & Settlement", "8086", "/actuator/health"]
];

function App() {
  const openGateway = () => window.open("http://localhost:8080/actuator/health", "_blank");
  return (
    <main className="dashboard">
      <section className="hero">
        <span className="eyebrow">REFERENCE IMPLEMENTATION</span>
        <h1>Enterprise Payment Platform</h1>
        <p>One-command local environment for demonstrating a synthetic payment-processing architecture.</p>
        <button onClick={openGateway}>Check API Gateway</button>
      </section>
      <section className="notice">
        <strong>Demo only.</strong> Never use real cardholder data or real payment credentials.
      </section>
      <section>
        <h2>Platform services</h2>
        <div className="grid">
          {services.map(([name, port, health]) => (
            <article className="card" key={name}>
              <div className="status">LOCAL</div>
              <h3>{name}</h3>
              <p>localhost:{port}</p>
              <a href={"http://localhost:" + port + health} target="_blank" rel="noreferrer">Health endpoint →</a>
            </article>
          ))}
        </div>
      </section>
      <section className="flow">
        <h2>Demo payment flow</h2>
        <p>Tokenization → Fraud evaluation → Authorization → Routing → Event publication → Clearing & settlement</p>
      </section>
    </main>
  );
}

export default App;