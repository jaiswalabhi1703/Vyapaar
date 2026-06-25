import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('demo');
  const [password, setPassword] = useState('password');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      if (mode === 'login') await login(username, password);
      else await register(username, password);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.error || 'Authentication failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-wrap">
      <div className="card pad auth-card">
        <h2>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h2>
        <p className="muted">{mode === 'login' ? 'Log in to place orders.' : 'Join in a few seconds.'}</p>

        <div className="social-row">
          <button className="btn btn-social" disabled title="Coming in the next update">
            <span className="g">G</span> Continue with Google
          </button>
          <button className="btn btn-social" disabled title="Coming in the next update">
            <span className="gh"></span> Continue with GitHub
          </button>
          <div className="soon-note">Social login is being wired up next ✨</div>
        </div>

        <div className="divider"><span>or</span></div>

        <form onSubmit={submit}>
          <label className="field">Username
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
          </label>
          <label className="field">Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </label>
          {error && <div className="alert error">{error}</div>}
          <button className="btn btn-primary full" disabled={busy}>
            {busy ? '…' : mode === 'login' ? 'Log in' : 'Register'}
          </button>
        </form>

        <p className="muted switch">
          {mode === 'login' ? "No account?" : 'Have an account?'}{' '}
          <button className="link-btn" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? 'Register' : 'Log in'}
          </button>
        </p>
        <p className="muted tiny">Try <code>demo / password</code> · admin <code>admin / admin123</code></p>
      </div>
    </div>
  );
}
