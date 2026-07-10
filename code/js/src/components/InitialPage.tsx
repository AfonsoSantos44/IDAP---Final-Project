import { useNavigate } from 'react-router-dom';
import '../styles/InitialPage.css';

/**
 * InitialPage component.
 * 
 * @return Simple initial page with auth buttons
 */

function InitialPage() {
    const navigate = useNavigate();

    return (
        <div className="homepage-wrapper">
            <div className="homepage-overlay">
                <div className="homepage-content">
                    <h1 className="homepage-title">IDAP</h1>
                    <p className="homepage-subtitle">Sistema de apoio à análise e comparação de danos em sinistros automóveis.</p>
                    <div className="homepage-buttons">
                        <button 
                            className="homepage-btn login-btn"
                            onClick={() => navigate('/login')}
                        >
                            Entrar
                        </button>
                        <button 
                            className="homepage-btn register-btn"
                            onClick={() => navigate('/register')}
                        >
                            Registar
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default InitialPage;