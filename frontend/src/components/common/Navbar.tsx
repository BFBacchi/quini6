import { Link, useLocation } from 'react-router-dom';

const API_BASE = 'http://localhost:8082';

export function Navbar() {
  const location = useLocation();

  const links = [
    { path: '/', label: 'Dashboard' },
    { path: '/frecuencia', label: 'Heatmap' },
    { path: '/generador', label: 'Generador' },
    { path: '/sorteos', label: 'Sorteos' },
    { path: '/monitoring', label: 'Monitoring' },
  ];

  const externalLinks = [
    { href: `${API_BASE}/swagger-ui.html`, label: 'API Docs', icon: '&#128214;' },
    { href: 'http://localhost:3001', label: 'Grafana', icon: '&#128200;' },
    { href: 'http://localhost:9090', label: 'Prometheus', icon: '&#128202;' },
    { href: 'http://localhost:5601', label: 'Kibana', icon: '&#128269;' },
  ];

  return (
    <nav className="navbar">
      <Link to="/" style={{ fontSize: '1.25rem', fontWeight: 700, color: '#4361ee' }}>
        Quini6 Analytics
      </Link>
      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
        {links.map((link) => (
          <Link
            key={link.path}
            to={link.path}
            style={{
              background: location.pathname === link.path ? '#4361ee' : undefined,
              color: location.pathname === link.path ? 'white' : undefined,
            }}
          >
            {link.label}
          </Link>
        ))}
        <span style={{ margin: '0 0.25rem', color: '#ccc' }}>|</span>
        {externalLinks.map((link) => (
          <a
            key={link.href}
            href={link.href}
            target="_blank"
            rel="noopener noreferrer"
            title={link.label}
            style={{
              background: '#f0f0f0',
              color: '#333',
              padding: '0.35rem 0.6rem',
              borderRadius: '6px',
              fontSize: '0.8rem',
              textDecoration: 'none',
              display: 'flex',
              alignItems: 'center',
              gap: '0.3rem',
            }}
          >
            <span dangerouslySetInnerHTML={{ __html: link.icon }} />
            {link.label}
          </a>
        ))}
      </div>
    </nav>
  );
}
