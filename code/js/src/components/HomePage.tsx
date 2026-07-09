import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/HomePage.css';

export default function HomePage() {
    const { logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate('/initial');
    }

    return (
        <div className="homepage-wrapper">
            <div className="homepage-overlay">
                <div className="logout-wrapper">
                    <button className="logout-btn" onClick={handleLogout}>Logout</button>
                </div>

                <div className="homepage-content">
                    <h1 className="homepage-title">IDAP</h1>
                    <p className="homepage-subtitle">Sistema de apoio à análise e comparação de danos em sinistros automóveis.</p>

                    <div className="homepage-buttons">
                        <Link to="/cases">
                            <button className="homepage-btn login-btn">Meus Casos</button>
                        </Link>

                        <Link to="/profile">
                            <button className="homepage-btn login-btn">Meu Perfil</button>
                        </Link>

                        <Link to="/reports">
                            <button className="homepage-btn login-btn">Relatórios</button>
                        </Link>

                        <Link to="/about">
                            <button className="homepage-btn login-btn">Sobre o projeto</button>
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
