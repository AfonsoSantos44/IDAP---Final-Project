import { useState } from "react";
import { Link } from "react-router-dom";
import { userService } from "../../services/userService";
import { useNavigate } from "react-router-dom";
import "../../styles/RegisterPage.css";
type RegisterPage = {
  username: string;
  email: string;
  password: string;
  error: string | undefined;
};

function RegisterPage() {
  const [state, setState] = useState<RegisterPage>({ username: "", email: "", password: "", error: undefined });
  const navigate = useNavigate();
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setState(prev => ({ ...prev, error: undefined }));
    try {
      await userService.register({
        username: state.username,
        email: state.email,
        password: state.password,
      });
      navigate("/login");
    } catch (err: any) {
      const message = err?.message || err?.title || "Erro ao registar";
      setState(prev => ({ ...prev, error: message }));
    }
  }
  return (
    
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2>Criar Conta</h2>
        <p>Regista-te para começar</p>
        
        {state.error && <div className="auth-error">{state.error}</div>}
        
        <form  className="auth-form">
          <div className="auth-form-group">
            <label>Nome de Utilizador</label>
            <input
              type="text"
              placeholder="Escolhe um nome de utilizador"
              value={state.username}
              onChange={(e) => setState({ ...state, username: e.target.value })}
              required
            />
          </div>
          
          <div className="auth-form-group">
            <label>Email</label>
            <input
              type="email"
              placeholder="Insere o teu email"
              value={state.email}
              onChange={(e) => setState({ ...state, email: e.target.value })}
              required
            />
          </div>
          
          <div className="auth-form-group">
            <label>Palavra-passe</label>
            <input
              type="password"
              placeholder="Cria uma palavra-passe forte"
              value={state.password}
              onChange={(e) => setState({ ...state, password: e.target.value })}
              required
            />
            </div>
        
          <button
            type="submit"
            className="auth-submit-btn"
            onClick={handleSubmit}
          >
            Registar
          </button>
        </form>
        
        <div className="auth-footer">
          Já tens uma conta? <Link to="/login">Inicia sessão</Link>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;