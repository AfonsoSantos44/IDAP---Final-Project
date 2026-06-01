import React,{useReducer, useState} from "react";
import { userService } from "../../services/userService";
import { Link, useNavigate } from "react-router-dom";
import "../../styles/LoginPage.css";


type Login = {
  username: string;
  password: string;
  error: string | undefined;
};

function LoginPage() {
    const [state, setState] = useState<Login>({ username: "", password: "", error: undefined });
return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2>Bem-vindo</h2>
        <p>Entra na tua conta</p>
        
        {state.error && <div className="auth-error">{state.error}</div>}
        
        <form className="auth-form">
          <div className="auth-form-group">
            <label>Nome de Utilizador</label>
            <input
              type="text"
              name="username"
              placeholder="Introduz o teu nome de utilizador"
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
              placeholder="Introduz a tua palavra-passe"
              required
              autoComplete="current-password"
              onChange={(e) => setState({ ...state, password: e.target.value })}
            />
          </div>
          
          <button
            type="submit"
            className="auth-submit-btn"
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