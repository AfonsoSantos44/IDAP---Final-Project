import { CreateCaseInput } from '../../types/caseTypes';
import '../../styles/CreateCasePage.css';

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';
import { caseService } from '../../services/caseService';
import { User } from '../../types/userTypes';

function CreateCasePage() {
    const navigate = useNavigate();
    const { logout } = useAuth();

    const handleLogout = async () => {
        await logout();
        navigate('/initial');
    }

    const [input, setInput] = useState<CreateCaseInput>({
        user: 0,
        description: '',
        status: '',
    });

    const [users, setUsers] = useState<User[]>([]);
    const [loadingUsers, setLoadingUsers] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        const loadUsers = async () => {
            setLoadingUsers(true);
            try {
                const data = await userService.getAllUsers();
                setUsers(data);
            } catch (err) {
                console.error('Failed to load users', err);
                setError('Falha ao obter utilizadores.');
            } finally {
                setLoadingUsers(false);
            }
        };

        loadUsers();
    }, []);

    const handleInputChange = (field: keyof CreateCaseInput, value: any) => {
        setInput((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = async () => {
        if (submitting) return; // prevent double submit
        setError(null);
        if (!input.user || input.user === 0) {
            setError('Escolha um averiguador válido.');
            return;
        }

        setSubmitting(true);
        console.log('Creating case (payload):', input);
        try {
            await caseService.createCase(input);
            console.log('Create case succeeded');
            navigate('/cases');
        } catch (err: any) {
            console.error('Create case failed', err);
            setError(err?.message || 'Falha ao criar caso.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="create-case-page">
            <div className="logout-wrapper">
                <button className="logout-btn" onClick={handleLogout}>Logout</button>
            </div>
            <button onClick={() => { window.location.href = '/cases'; }}>Voltar</button>
            <h1>Criar um novo caso</h1>
            {error && <div className="error-message">{error}</div>}
            <form onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
                <div className="form-group">
                    <label htmlFor="user">Averiguador:</label>
                    {loadingUsers ? (
                        <div>Carregando utilizadores...</div>
                    ) : (
                        <select
                            id="user"
                            value={String(input.user)}
                            onChange={(e) => handleInputChange('user', e.target.value)}
                        >
                            <option value={"0"}> Seleciona um averiguador</option>
                            {users.map((u) => (
                                <option key={u.id} value={String(u.id)}>{u.username || u.email}</option>
                            ))}
                        </select>
                    )}
                </div>

                <div className="form-group">
                    <label htmlFor="description">Descrição:</label>
                    <textarea
                        id="description"
                        placeholder="Insere uma descrição do caso"
                        value={input.description}
                        onChange={(e) => handleInputChange('description', e.target.value)}
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="status">Estado:</label>
                    <select
                        id="status"
                        value={input.status}
                        onChange={(e) => handleInputChange('status', e.target.value)}
                    >
                        <option value=""> Seleciona o estado do caso </option>
                        <option value="open">Aberto</option>
                        <option value="in_progress">Em progresso</option>
                        <option value="closed">Fechado</option>
                    </select>
                </div>

                <button type="submit" disabled={submitting}>
                    {submitting ? 'A criar...' : 'Criar Caso'}
                </button>
            </form>
        </div>
    );
};

export default CreateCasePage;
