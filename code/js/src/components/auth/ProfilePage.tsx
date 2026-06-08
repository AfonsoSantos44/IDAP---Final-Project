import React, { useEffect, useState } from 'react';
import '../../styles/ProfilePage.css';
import { userService } from '../../services/userService';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

type CurrentUser = {
  userId?: number;
  id?: number;
  username?: string;
  name?: string;
  email?: string;
};

export default function ProfilePage() {
  const { logout } = useAuth();
  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  }
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const data = await userService.getMe();
        setUser(data as any);
      } catch (err: any) {
        setError(err?.message || 'Erro ao obter utilizador atual');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <div className="profile-wrapper"><div className="profile-card">A carregar...</div></div>;
  if (error) return <div className="profile-wrapper"><div className="profile-card error">Erro: {error}</div></div>;

  const id = user?.userId ?? user?.id ?? '-';
  const username = user?.username ?? user?.name ?? '-';
  const email = user?.email ?? '-';

  return (
    <div className="profile-wrapper">
      <div className="logout-wrapper">
        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>
      <div className="profile-card">
        <h1 className="profile-title">Perfil</h1>
        <div className="profile-row"><strong>Nome:</strong> <span>{username}</span></div>
        <div className="profile-row"><strong>Email:</strong> <span>{email}</span></div>

        <div className="profile-actions">
          <button className="page-btn" onClick={() => navigate(-1)}>Voltar</button>
        </div>
      </div>
    </div>
  );
}
