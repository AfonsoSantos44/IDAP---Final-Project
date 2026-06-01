import { useState } from "react";
import { Link } from "react-router-dom";
import "../../styles/RegisterPage.css";
type Register = {
  username: string;
  password: string;
  error: string | undefined;
};

function Register() {
  const [state, setState] = useState<Register>({ username: "", password: "", error: undefined });

  return (
    
    <div className="auth-wrapper">
      {/* Animated background blobs */}
      <div className="auth-card">
        <h2>Criar Conta</h2>
        <p>Regista-te para começar</p>
        
        {state.error && <div className="auth-error">{state.error}</div>}
        
        <form  className="auth-form">
          <div className="auth-form-group">
            <label>Username</label>
            <input
              type="text"
              placeholder="Escolhe um username"
              value={state.username}
              onChange={(e) => setState({ ...state, username: e.target.value })}
              required
            />
          </div>
          
          <div className="auth-form-group">
            <label>Password</label>
            <input
              type="password"
              placeholder="Cria uma password forte"
              value={state.password}
              onChange={(e) => setState({ ...state, password: e.target.value })}
              required
            />
            </div>
        
          <button
            type="submit"
            className="auth-submit-btn"
          >
            Register
          </button>
        </form>
        
        <div className="auth-footer">
          Já tens uma conta? <Link to="/login">Inicia sessão</Link>
        </div>
      </div>
    </div>
  );
}

export default Register;