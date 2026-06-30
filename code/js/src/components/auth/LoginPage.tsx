import React,{useReducer, useState} from "react";
import { userService } from "../../services/userService";
import { Link, useNavigate } from "react-router-dom";
import "../../styles/LoginPage.css";
import { useAuth } from "../../context/AuthContext";


type Login = {
  username: string;
  password: string;
  error: string | undefined;
};

function LoginPage() {
  const [state, setState] = useState<Login>({ username: "", password: "", error: undefined });
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e: React.MouseEvent<HTMLButtonElement> | React.FormEvent) => {
    e.preventDefault();
      setState(prev => ({ ...prev, error: undefined }));
    try {
      // backend expects email in the login model; we map username field to email
      const user = await userService.login({ email: state.username, password: state.password });
      login("authenticated", String(user.userId));
      navigate("/home");
    } catch (err: any) {
      const message = err?.message || err?.title || "Erro ao iniciar sessão";
      setState(prev => ({ ...prev, error: message }));
    }
  }
return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2>Bem-vindo</h2>
        <p>Entra na tua conta</p>
        
        {state.error && <div className="auth-error">{state.error}</div>}
        
        <form className="auth-form">
          <div className="auth-form-group">
            <label>Email</label>
            <input
              type="text"
              name="username"
              placeholder="Insere o teu email"
              required
              autoComplete="username"
              onChange={(e) => setState({ ...state, username: e.target.value })}
            />
          </div>
          
          <div className="auth-form-group">
            <label>Palavra-passe</label>
            <input
              type="password"
              name="password"
              placeholder="Insere a tua palavra-passe"
              required
              autoComplete="current-password"
              onChange={(e) => setState({ ...state, password: e.target.value })}
            />
          </div>
          
          <button
            type="button"
            className="auth-submit-btn"
            onClick={handleSubmit}
          >
            Login
          </button>
        </form>
        
        <div className="auth-footer">
          Não tens uma conta? <Link to="/register">Regista-te</Link>
        </div>
      </div>
    </div>
  );
}


export default LoginPage;
