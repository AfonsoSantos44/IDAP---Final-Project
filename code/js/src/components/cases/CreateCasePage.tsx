import { CreateCaseInput } from '../../types/caseTypes';
import '../../styles/CreateCasePage.css';

import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';
import { caseService } from '../../services/caseService';
import { CreateVehicleRequest, vehicleService } from '../../services/vehicleService';
import { User } from '../../types/userTypes';

type VehicleFormInput = {
    brand: string;
    model: string;
    yearOfFabrication: string;
    licensePlate: string;
    color: string;
    role: string;
};

const emptyVehicleForm: VehicleFormInput = {
    brand: '',
    model: '',
    yearOfFabrication: '',
    licensePlate: '',
    color: '',
    role: '',
};

type PendingVehicle = CreateVehicleRequest & {
    localId: number;
};

type DropdownOption = {
    value: string;
    label: string;
};

function StyledDropdown({
    id,
    value,
    placeholder,
    options,
    onChange,
}: {
    id: string;
    value: string;
    placeholder: string;
    options: DropdownOption[];
    onChange: (value: string) => void;
}) {
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement | null>(null);
    const selectedLabel = options.find((option) => option.value === value)?.label || placeholder;

    useEffect(() => {
        const closeDropdown = (event: MouseEvent) => {
            if (!dropdownRef.current?.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', closeDropdown);
        return () => document.removeEventListener('mousedown', closeDropdown);
    }, []);

    return (
        <div className="create-case-dropdown" ref={dropdownRef}>
            <button
                id={id}
                type="button"
                className={`create-case-dropdown-trigger ${isOpen ? 'open' : ''}`}
                aria-haspopup="listbox"
                aria-expanded={isOpen}
                onClick={() => setIsOpen((open) => !open)}
            >
                <span>{selectedLabel}</span>
                <span className="create-case-dropdown-chevron">▾</span>
            </button>

            {isOpen && (
                <div className="create-case-dropdown-menu" role="listbox" aria-labelledby={id}>
                    {options.map((option) => (
                        <button
                            key={option.value}
                            type="button"
                            className={option.value === value ? 'selected' : ''}
                            role="option"
                            aria-selected={option.value === value}
                            onClick={() => {
                                onChange(option.value);
                                setIsOpen(false);
                            }}
                        >
                            {option.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}

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
    const [showVehicleModal, setShowVehicleModal] = useState(false);
    const [vehicleInput, setVehicleInput] = useState<VehicleFormInput>(emptyVehicleForm);
    const [vehicleError, setVehicleError] = useState<string | null>(null);
    const [vehicles, setVehicles] = useState<PendingVehicle[]>([]);

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
            const createdCase = await caseService.createCase(input);
            const nextCaseId = createdCase.caseId ?? createdCase.id;
            if (!nextCaseId) {
                throw new Error('Caso criado, mas nao foi possivel obter o ID.');
            }

            await Promise.all(
                vehicles.map(({ localId, ...vehicle }) =>
                    vehicleService.createCaseVehicle(Number(nextCaseId), vehicle)
                )
            );

            console.log('Create case succeeded');
            navigate(`/cases/${Number(nextCaseId)}`);
        } catch (err: any) {
            console.error('Create case failed', err);
            setError(err?.message || 'Falha ao criar caso.');
        } finally {
            setSubmitting(false);
        }
    };

    const handleVehicleInputChange = (field: keyof VehicleFormInput, value: string) => {
        setVehicleInput((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const validateVehicle = (): string | null => {
        if (!vehicleInput.brand.trim()) return 'A marca do veículo e obrigatória.';
        if (!vehicleInput.model.trim()) return 'O modelo do veículo e obrigatório.';
        if (!vehicleInput.licensePlate.trim()) return 'A matrícula do veículo e obrigatória.';

        const year = Number(vehicleInput.yearOfFabrication);
        if (!Number.isInteger(year)) return 'O ano  deve ser um número inteiro.';
        if (year < 1900 || year > new Date().getFullYear() + 1) {
            return 'Insira um ano válido.';
        }

        return null;
    };

    const handleVehicleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setVehicleError(null);

        const validationError = validateVehicle();
        if (validationError) {
            setVehicleError(validationError);
            return;
        }

        setVehicles((prev) => [
            ...prev,
            {
                localId: Date.now(),
                brand: vehicleInput.brand.trim(),
                model: vehicleInput.model.trim(),
                yearOfFabrication: Number(vehicleInput.yearOfFabrication),
                licensePlate: vehicleInput.licensePlate.trim(),
                color: vehicleInput.color.trim() || null,
                role: vehicleInput.role.trim() || null,
            },
        ]);
        setVehicleInput(emptyVehicleForm);
        setShowVehicleModal(false);
    };

    const openVehicleModal = () => {
        setVehicleInput(emptyVehicleForm);
        setVehicleError(null);
        setShowVehicleModal(true);
    };

    const closeVehicleModal = () => {
        setVehicleInput(emptyVehicleForm);
        setVehicleError(null);
        setShowVehicleModal(false);
    };

    const removeVehicle = (localId: number) => {
        setVehicles((prev) => prev.filter((vehicle) => vehicle.localId !== localId));
    };

    const investigatorOptions = [
        { value: '', label: 'Seleciona um averiguador' },
        ...users.map((u) => {
            const uid = (u as any).id ?? (u as any).userId;
            const label = u.username || u.email || String(uid);

            return {
                value: label,
                label,
            };
        }),
    ];

    const statusOptions = [
        { value: '', label: 'Seleciona o estado do caso' },
        { value: 'open', label: 'Aberto' },
        { value: 'in_progress', label: 'Em progresso' },
        { value: 'closed', label: 'Fechado' },
    ];

    const handleInvestigatorChange = (selected: string) => {
        setAssignedUsername(selected);
        const found = users.find(u => {
            const uid = (u as any).id ?? (u as any).userId;
            return (u.username || u.email || String(uid)) === selected;
        });
        console.debug('CreateCase select change, selected=', selected, 'found=', found);
        if (found) {
            const fid = (found as any).id ?? (found as any).userId;
            setInput(prev => ({ ...prev, user: Number(fid) }));
        } else {
            setInput(prev => ({ ...prev, user: 0 }));
        }
    };

    return (
        <div className="create-case-page">
            <div className="logout-wrapper">
                <button className="logout-btn" onClick={handleLogout}>Logout</button>
            </div>
            <button onClick={() => { navigate('/cases'); }}>Voltar</button>
            <h1>Criar um novo caso</h1>
            {error && <div className="error-message">{error}</div>}
            <form onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
                <div className="form-group">
                    <label htmlFor="user">Averiguador:</label>
                    {loadingUsers ? (
                        <div>Carregando utilizadores...</div>
                    ) : (
                        isAdmin ? (
                            <StyledDropdown
                                id="user"
                                value={assignedUsername}
                                placeholder="Seleciona um averiguador"
                                options={investigatorOptions}
                                onChange={handleInvestigatorChange}
                            />
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
                    <StyledDropdown
                        id="status"
                        value={input.status}
                        placeholder="Seleciona o estado do caso"
                        options={statusOptions}
                        onChange={(value) => handleInputChange('status', value)}
                    />
                </div>

                <section className="vehicles-section">
                    <div className="vehicles-section-header">
                        <div>
                            <h2>Veículos envolvidos</h2>
                            <p>{vehicles.length === 0 ? 'Ainda não existem veículos adicionados.' : `${vehicles.length} veiculo(s) adicionados.`}</p>
                        </div>
                        <button type="button" className="add-vehicle-button" onClick={openVehicleModal}>
                            Adicionar veículo
                        </button>
                    </div>

                    <div className={`vehicles-list ${vehicles.length > 4 ? 'scrollable' : ''}`}>
                        {vehicles.length === 0 ? (
                            <div className="vehicles-empty">A lista esta vazia.</div>
                        ) : (
                            vehicles.map((vehicle) => (
                                <div className="vehicle-list-item" key={vehicle.localId}>
                                    <div>
                                        <strong>{vehicle.brand} {vehicle.model}</strong>
                                        <span>{vehicle.licensePlate} - {vehicle.yearOfFabrication}{vehicle.color ? ` - ${vehicle.color}` : ''}</span>
                                        {vehicle.role && <small>{vehicle.role}</small>}
                                    </div>
                                    <button type="button" onClick={() => removeVehicle(vehicle.localId)}>
                                        Remover
                                    </button>
                                </div>
                            ))
                        )}
                    </div>
                </section>

                <button type="submit" disabled={submitting}>
                    {submitting ? 'A criar...' : 'Criar Caso'}
                </button>
            </form>

            {showVehicleModal && (
                <div className="vehicle-modal-backdrop" role="dialog" aria-modal="true">
                    <div className="vehicle-modal">
                        <h2>Adicionar veículo ao caso</h2>
                        <p>Preencha os dados do veículo para o adicionar à lista deste caso.</p>

                        {vehicleError && <div className="error-message">{vehicleError}</div>}

                        <form onSubmit={handleVehicleSubmit} className="vehicle-modal-form">
                            <div className="vehicle-form-grid">
                                <div className="form-group">
                                    <label htmlFor="vehicle-brand">Marca:</label>
                                    <input
                                        id="vehicle-brand"
                                        value={vehicleInput.brand}
                                        onChange={(e) => handleVehicleInputChange('brand', e.target.value)}
                                        placeholder="Ex.: Toyota"
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="vehicle-model">Modelo:</label>
                                    <input
                                        id="vehicle-model"
                                        value={vehicleInput.model}
                                        onChange={(e) => handleVehicleInputChange('model', e.target.value)}
                                        placeholder="Ex.: Corolla"
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="vehicle-year">Ano:</label>
                                    <input
                                        id="vehicle-year"
                                        type="number"
                                        min="1900"
                                        max={new Date().getFullYear() + 1}
                                        value={vehicleInput.yearOfFabrication}
                                        onChange={(e) => handleVehicleInputChange('yearOfFabrication', e.target.value)}
                                        placeholder="Ex.: 2020"
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="vehicle-license">Matrícula:</label>
                                    <input
                                        id="vehicle-license"
                                        value={vehicleInput.licensePlate}
                                        onChange={(e) => handleVehicleInputChange('licensePlate', e.target.value)}
                                        placeholder="Ex.: AA-00-BB"
                                    />
                                </div>

                                <div className="form-group">
                                    <label htmlFor="vehicle-color">Cor:</label>
                                    <input
                                        id="vehicle-color"
                                        value={vehicleInput.color}
                                        onChange={(e) => handleVehicleInputChange('color', e.target.value)}
                                        placeholder="Ex.: Vermelho"
                                    />
                                </div>

                                <div className="form-group wide">
                                    <label htmlFor="vehicle-role">Papel no acidente:</label>
                                    <input
                                        id="vehicle-role"
                                        value={vehicleInput.role}
                                        onChange={(e) => handleVehicleInputChange('role', e.target.value)}
                                        placeholder="Ex.: Veículo interveniente"
                                    />
                                </div>
                            </div>

                            <div className="vehicle-modal-actions">
                                <button type="button" className="secondary-action" onClick={closeVehicleModal}>
                                    Cancelar
                                </button>
                                <button type="submit">
                                    Adicionar veículo
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CreateCasePage;
