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
    const { logout, userId: currentUserId, isLoading: authLoading } = useAuth();

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
    const [isAdmin, setIsAdmin] = useState<boolean>(false);
    const [assignedUsername, setAssignedUsername] = useState<string>('');
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        const loadUsers = async () => {
            setLoadingUsers(true);
            try {
                // try to list all users - only allowed to admins
                const data = await userService.getAllUsers();
                setUsers(data);
                setIsAdmin(true);
                // if current user present, preselect them in the UI
                                if (currentUserId) {
                                    const me = data.find((u: any) => String((u.id ?? u.userId)) === String(currentUserId));
                                    if (me) {
                                        const meId = (me as any).id ?? (me as any).userId;
                                        setAssignedUsername(me.username || me.email || String(meId));
                                        setInput((prev) => ({ ...prev, user: Number(meId) }));
                                    }
                                }
            } catch (err) {
                // non-admins will receive 401/403; treat as non-admin
                console.debug('User is not admin or failed to list users', err);
                setUsers([]);
                setIsAdmin(false);
                // fetch current user info to show username
                try {
                    const me = await userService.getMe();
                    const meId = (me as any).id ?? (me as any).userId;
                    setAssignedUsername(me.name || me.email || String(meId));
                } catch (_) {
                    // ignore
                }
                // if not admin, default the case reporter to current user id
                if (currentUserId) {
                    setInput((prev) => ({ ...prev, user: Number(currentUserId) }));
                }
            } finally {
                setLoadingUsers(false);
            }
        };

        if (!authLoading) loadUsers();
    }, [authLoading, currentUserId]);

    const handleInputChange = (field: keyof CreateCaseInput, value: any) => {
        setInput((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = async () => {
        if (submitting) return; // prevent double submit
        setError(null);
        console.debug('Submitting create case, input.user=', input.user, 'assignedUsername=', assignedUsername);
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
                        isAdmin ? (
                            <select
                                id="user"
                                value={assignedUsername}
                                onChange={(e) => {
                                    const selected = e.target.value;
                                    setAssignedUsername(selected);
                                    const found = users.find(u => {
                                        const uid = (u as any).id ?? (u as any).userId;
                                        return (u.username || u.email || String(uid)) === selected;
                                    });
                                    console.debug('CreateCase select change, selected=', selected, 'found=', found);
                                    if (found) {
                                        const fid = (found as any).id ?? (found as any).userId;
                                        setInput(prev => ({ ...prev, user: Number(fid) }));
                                    } else setInput(prev => ({ ...prev, user: 0 }));
                                }}
                            >
                                <option value={""}> Seleciona um averiguador</option>
                                {users.map((u) => {
                                    const uid = (u as any).id ?? (u as any).userId;
                                    return (
                                        <option key={uid} value={u.username || u.email || String(uid)}>{u.username || u.email}</option>
                                    );
                                })}
                            </select>
                        ) : (
                            <div>
                                {currentUserId ? `Averiguador atual: #${currentUserId}` : 'Averiguador: (não disponível)'}
                                <input type="hidden" name="user" value={String(input.user)} />
                            </div>
                        )
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
