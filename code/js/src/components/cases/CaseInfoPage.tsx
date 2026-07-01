import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/CaseInfoPage.css';
import { caseService } from '../../services/caseService';
import { userService } from '../../services/userService';
import analysisService from '../../services/analysisService';
import { vehicleService, type CreateDamageRequest, type CreateVehicleRequest, type DamageOutput, type VehicleOutput } from '../../services/vehicleService';

type VehicleFormInput = {
	brand: string;
	model: string;
	yearOfFabrication: string;
	licensePlate: string;
	role: string;
};

type DamageFormInput = {
	contactZone: string;
	deformationType: string;
	direction: string;
	damageDescription: string;
};

const emptyVehicleForm: VehicleFormInput = {
	brand: '',
	model: '',
	yearOfFabrication: '',
	licensePlate: '',
	role: '',
};

const emptyDamageForm: DamageFormInput = {
	contactZone: '',
	deformationType: '',
	direction: '',
	damageDescription: '',
};

function CaseInfoPage() {
	const navigate = useNavigate();
	const { logout } = useAuth();

	const handleLogout = async () => {
		await logout();
		navigate('/initial');
	}
	const { id, caseId } = useParams<{ id: string; caseId: string }>();
	const currentCaseId = id ?? caseId;

	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [caseData, setCaseData] = useState<any | null>(null);
	const [reporterName, setReporterName] = useState<string | undefined>(undefined);
	const [vehicles, setVehicles] = useState<VehicleOutput[]>([]);
	const [showVehicleModal, setShowVehicleModal] = useState(false);
	const [vehicleInput, setVehicleInput] = useState<VehicleFormInput>(emptyVehicleForm);
	const [vehicleError, setVehicleError] = useState<string | null>(null);
	const [submittingVehicle, setSubmittingVehicle] = useState(false);
	const [selectedDamageVehicle, setSelectedDamageVehicle] = useState<VehicleOutput | null>(null);
	const [damageInput, setDamageInput] = useState<DamageFormInput>(emptyDamageForm);
	const [damageError, setDamageError] = useState<string | null>(null);
	const [submittingDamage, setSubmittingDamage] = useState(false);
	const [selectedDetailsVehicle, setSelectedDetailsVehicle] = useState<VehicleOutput | null>(null);
	const [vehicleDamages, setVehicleDamages] = useState<DamageOutput[]>([]);
	const [loadingDamages, setLoadingDamages] = useState(false);
	const [damagesError, setDamagesError] = useState<string | null>(null);

	useEffect(() => {
		const load = async () => {
			if (!currentCaseId) return;
			setLoading(true);
			setError(null);
			try {
				const data = await caseService.getCase(Number(currentCaseId));
				// data shape may vary; normalize
				const normalized = {
					id: data.id ?? data.caseId ?? currentCaseId,
					description: data.description ?? data.accidentDescription ?? data.description,
					status: data.status ?? data.caseStatus ?? data.state,
					userId: data.userId ?? data.user ?? data.reporter,
					createdAt: data.createdAt ?? data.created_at ?? undefined,
				};
				setCaseData(normalized);
				try {
					const caseVehicles = await vehicleService.getCaseVehicles(Number(normalized.id));
					setVehicles(caseVehicles);
				} catch (e) {
					console.debug('Failed to load vehicles', e);
					setVehicles([]);
				}
				// try to fetch reporter name if present
				const userId = normalized.userId;
				if (userId) {
					try {
						const u = await userService.getUserById(Number(userId));
						setReporterName(u.name || u.username || u.email);
					} catch (e) {
						console.debug('Failed to load reporter name', e);
					}
				}
			} catch (e: any) {
				setError(e?.message || 'Erro ao carregar caso');
			} finally {
				setLoading(false);
			}
		};

		load();
	}, [currentCaseId]);

	const caseEntries = caseData
		? Object.entries({
			Descrição: caseData.description,
			Estado: caseData.status,
			Averiguador: reporterName ?? caseData.userId ?? '-',
			Criado: caseData.createdAt,
		}) as [string, unknown][]
		: [];

	const mapStatusClass = (status?: string) => {
		if (!status) return 'Pendente';
		const s = String(status).toLowerCase();
		if (s === 'open' || s === 'aberto') return 'Aberto';
		if (s === 'in_progress' || s === 'in-progress' || s === 'in progress' || s === 'em progresso') return 'EmProgresso';
		if (s === 'closed' || s === 'fechado') return 'Fechado';
		return 'Pendente';
	};

	const mapStatusLabel = (status?: string) => {
		if (!status) return 'Pendente';
		const s = String(status).toLowerCase();
		if (s === 'open' || s === 'aberto') return 'Aberto';
		if (s === 'in_progress' || s === 'in-progress' || s === 'in progress' || s === 'em progresso') return 'Em Progresso';
		if (s === 'closed' || s === 'fechado') return 'Fechado';
		return 'Pendente';
	};

	const formatVehicleLabel = (vehicle: VehicleOutput) => {
		const brandModel = [vehicle.brand, vehicle.model].filter(Boolean).join(' ');
		return brandModel || `Veiculo #${vehicle.vehicleId ?? '-'}`;
	};

	const handleVehicleInputChange = (field: keyof VehicleFormInput, value: string) => {
		setVehicleInput((prev) => ({
			...prev,
			[field]: value,
		}));
	};

	const validateVehicle = (): string | null => {
		if (!vehicleInput.brand.trim()) return 'A marca do veiculo Ã© obrigatÃ³ria.';
		if (!vehicleInput.model.trim()) return 'O modelo do veiculo Ã© obrigatÃ³rio.';
		if (!vehicleInput.licensePlate.trim()) return 'A matricula do veiculo Ã© obrigatÃ³ria.';

		const year = Number(vehicleInput.yearOfFabrication);
		if (!Number.isInteger(year)) return 'O ano de fabricaÃ§Ã£o deve ser um nÃºmero inteiro.';
		if (year < 1900 || year > new Date().getFullYear() + 1) {
			return 'Insira um ano de fabricacao valido.';
		}

		return null;
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

	const handleVehicleSubmit = async (event: React.FormEvent) => {
		event.preventDefault();
		setVehicleError(null);

		const validationError = validateVehicle();
		if (validationError) {
			setVehicleError(validationError);
			return;
		}

		const targetCaseId = caseData?.id ?? currentCaseId;
		if (!targetCaseId) {
			setVehicleError('NÃ£o foi possÃ­vel associar o veÃ­culo ao caso.');
			return;
		}

		const vehicle: CreateVehicleRequest = {
			brand: vehicleInput.brand.trim(),
			model: vehicleInput.model.trim(),
			yearOfFabrication: Number(vehicleInput.yearOfFabrication),
			licensePlate: vehicleInput.licensePlate.trim(),
			role: vehicleInput.role.trim() || null,
		};

		try {
			setSubmittingVehicle(true);
			const createdVehicle = await vehicleService.createCaseVehicle(Number(targetCaseId), vehicle);
			setVehicles((prev) => [...prev, createdVehicle]);
			closeVehicleModal();
		} catch (err: any) {
			setVehicleError(err?.message || 'Falha ao adicionar veÃ­culo.');
		} finally {
			setSubmittingVehicle(false);
		}
	};

	const handleRemoveVehicle = async (vehicle: VehicleOutput) => {
		if (!vehicle.vehicleId) return;
		if (!confirm('Tem a certeza que deseja remover este veÃ­culo?')) return;

		try {
			await vehicleService.deleteVehicle(vehicle.vehicleId);
			setVehicles((prev) => prev.filter((item) => item.vehicleId !== vehicle.vehicleId));
		} catch (err: any) {
			alert(err?.message || 'Falha ao remover veÃ­culo.');
		}
	};

	const openVehicleDetailsModal = async (vehicle: VehicleOutput) => {
		if (!vehicle.vehicleId) return;

		setSelectedDetailsVehicle(vehicle);
		setVehicleDamages([]);
		setDamagesError(null);
		setLoadingDamages(true);

		try {
			const damages = await vehicleService.getVehicleDamages(vehicle.vehicleId);
			setVehicleDamages(damages);
		} catch (err: any) {
			setDamagesError(err?.message || 'Falha ao carregar danos do veÃ­culo.');
		} finally {
			setLoadingDamages(false);
		}
	};

	const closeVehicleDetailsModal = () => {
		setSelectedDetailsVehicle(null);
		setVehicleDamages([]);
		setDamagesError(null);
		setLoadingDamages(false);
	};

	const handleRemoveDamage = async (damage: DamageOutput) => {
		if (!damage.damageId) return;
		if (!confirm('Tem a certeza que deseja remover este dano?')) return;

		try {
			await vehicleService.deleteDamage(damage.damageId);
			setVehicleDamages((prev) => prev.filter((item) => item.damageId !== damage.damageId));
		} catch (err: any) {
			setDamagesError(err?.message || 'Falha ao remover dano.');
		}
	};

	const openDamageModal = (vehicle: VehicleOutput) => {
		setSelectedDamageVehicle(vehicle);
		setDamageInput(emptyDamageForm);
		setDamageError(null);
	};

	const closeDamageModal = () => {
		setSelectedDamageVehicle(null);
		setDamageInput(emptyDamageForm);
		setDamageError(null);
	};

	const handleDamageInputChange = (field: keyof DamageFormInput, value: string) => {
		setDamageInput((prev) => ({
			...prev,
			[field]: value,
		}));
	};

	const validateDamage = (): string | null => {
		if (!damageInput.contactZone.trim()) return 'A zona de contacto Ã© obrigatÃ³ria.';
		if (!damageInput.deformationType.trim()) return 'O tipo de deformaÃ§Ã£o Ã© obrigatÃ³rio.';
		if (!damageInput.direction.trim()) return 'A direÃ§Ã£o Ã© obrigatÃ³ria.';
		if (!damageInput.damageDescription.trim()) return 'A descriÃ§Ã£o do dano Ã© obrigatÃ³ria.';
		return null;
	};

	const handleDamageSubmit = async (event: React.FormEvent) => {
		event.preventDefault();
		setDamageError(null);

		const validationError = validateDamage();
		if (validationError) {
			setDamageError(validationError);
			return;
		}

		if (!selectedDamageVehicle?.vehicleId) {
			setDamageError('NÃ£o foi possÃ­vel identificar o veÃ­culo.');
			return;
		}

		const damage: CreateDamageRequest = {
			contactZone: damageInput.contactZone.trim(),
			deformationType: damageInput.deformationType.trim(),
			direction: damageInput.direction.trim(),
			damageDescription: damageInput.damageDescription.trim(),
		};

		try {
			setSubmittingDamage(true);
			const createdDamage = await vehicleService.createVehicleDamage(selectedDamageVehicle.vehicleId, damage);
			if (selectedDetailsVehicle?.vehicleId === selectedDamageVehicle.vehicleId) {
				setVehicleDamages((prev) => [...prev, createdDamage]);
			}
			closeDamageModal();
		} catch (err: any) {
			setDamageError(err?.message || 'Falha ao criar dano.');
		} finally {
			setSubmittingDamage(false);
		}
	};

	const handleAnalyze = async () => {
		try {
			const targetCaseId = caseData?.id ?? currentCaseId;
			if (!targetCaseId) return;

			const analyses = await analysisService.listCaseAnalyses(Number(targetCaseId));

			if (analyses.length > 0) {
				navigate(`/cases/${targetCaseId}/analysis/${analyses[0].analysisId}`);
				return;
			}

			const analysis = await analysisService.createCaseAnalysis(Number(targetCaseId));
			navigate(`/cases/${targetCaseId}/analysis/${analysis.analysisId}`);
		} catch (err: any) {
			alert(err?.message || 'Erro ao criar anÃ¡lise');
		}
	};

	return (
		<div className="case-info-page">
			<div className="logout-wrapper">
				<button className="logout-btn" onClick={handleLogout}>Logout</button>
			</div>
			<div className="case-info-header">
				<button
					className="page-btn secondary back-btn"
					onClick={() => {
						window.location.href = '/cases';
					}}
				>
					Voltar
				</button>

				<h1 className="case-info-title">
					Informações do Caso
				</h1>
			</div>

			<section className="case-info-card">
				{loading && <div>Carregando caso...</div>}
				{error && <div style={{ color: 'red' }}>Erro: {error}</div>}
				{!loading && !error && caseEntries.map(([key, value]) => (
					<div className="case-info-row" key={key}>
						<strong className="case-info-label">
							{key}:
						</strong>

						<span
							className={
								key === 'Estado'
									? `case-status ${mapStatusClass(caseData?.status)}`
									: 'case-info-value'
							}
						>
							{key === 'Estado'
								? mapStatusLabel(caseData?.status)
								: (value === null || value === undefined
									? '-'
									: Array.isArray(value)
									? value.join(', ')
									: typeof value === 'object'
									? JSON.stringify(value, null, 2)
									: String(value))}
						</span>
					</div>
				))}

				{!loading && !error && (
					<div className="case-info-row vehicles-info-row">
						<div className="vehicles-info-header">
							<strong className="case-info-label">
								Veí­culos envolvidos:
							</strong>
							<button className="add-case-vehicle-btn" type="button" onClick={openVehicleModal}>
								Adicionar veí­culo
							</button>
						</div>

						<div className={`case-vehicles-list ${vehicles.length > 4 ? 'scrollable' : ''}`}>
							{vehicles.length === 0 ? (
								<span className="case-info-value">Ainda não existem veí­culos adicionados.</span>
							) : (
								vehicles.map((vehicle) => (
									<div className="case-vehicle-card" key={vehicle.vehicleId ?? `${vehicle.licensePlate}-${vehicle.model}`}>
										<div className="case-vehicle-details">
											<strong>{formatVehicleLabel(vehicle)}</strong>
											<span>{vehicle.licensePlate ?? 'Sem matrícula'} - {vehicle.yearOfFabrication ?? 'Ano desconhecido'}</span>
											{vehicle.role && <small>{vehicle.role}</small>}
										</div>
										<div className="case-vehicle-actions">
											<button
												className="vehicle-details-btn"
												type="button"
												onClick={() => openVehicleDetailsModal(vehicle)}
												disabled={!vehicle.vehicleId}
											>
												Detalhes
											</button>
											<button
												className="create-damage-btn"
												type="button"
												onClick={() => openDamageModal(vehicle)}
												disabled={!vehicle.vehicleId}
											>
												Criar dano
											</button>
											<button
												className="remove-vehicle-btn"
												type="button"
												onClick={() => handleRemoveVehicle(vehicle)}
												disabled={!vehicle.vehicleId}
											>
												Remover
											</button>
										</div>
									</div>
								))
							)}
						</div>
					</div>
				)}
			</section>

			<div className="case-menu-actions">
				<button
					className="case-menu-btn"
					type="button"
					onClick={() => {
						navigate(`/cases/${caseData?.id ?? currentCaseId}/evidences`);
					}}
				>
					Gerir evidências
				</button>

				<button
					className="case-menu-btn"
					type="button"
					onClick={handleAnalyze}
				>
				Analisar
				</button>
			</div>

			{selectedDetailsVehicle && (
				<div className="vehicle-modal-backdrop" role="dialog" aria-modal="true">
					<div className="vehicle-modal vehicle-details-modal">
						<div className="vehicle-details-modal-header">
							<div>
								<h2>{formatVehicleLabel(selectedDetailsVehicle)}</h2>
								<p>InformaÃ§Ã£o do veí­culo e danos associados.</p>
							</div>
							<button type="button" className="secondary-action" onClick={closeVehicleDetailsModal}>
								Fechar
							</button>
						</div>

						<div className="vehicle-details-grid">
							<div>
								<strong>Matrícula</strong>
								<span>{selectedDetailsVehicle.licensePlate ?? '-'}</span>
							</div>
							<div>
								<strong>Ano</strong>
								<span>{selectedDetailsVehicle.yearOfFabrication ?? '-'}</span>
							</div>
							<div>
								<strong>Marca</strong>
								<span>{selectedDetailsVehicle.brand ?? '-'}</span>
							</div>
							<div>
								<strong>Modelo</strong>
								<span>{selectedDetailsVehicle.model ?? '-'}</span>
							</div>
							<div className="wide">
								<strong>Papel no acidente</strong>
								<span>{selectedDetailsVehicle.role || '-'}</span>
							</div>
						</div>

						<section className="vehicle-damages-section">
							<h3>Danos associados</h3>

							{loadingDamages && <p className="vehicle-damages-empty">A carregar danos...</p>}
							{damagesError && <div className="vehicle-error-message">{damagesError}</div>}
							{!loadingDamages && !damagesError && vehicleDamages.length === 0 && (
								<p className="vehicle-damages-empty">Ainda não existem danos associados a este veículo.</p>
							)}

							{!loadingDamages && !damagesError && vehicleDamages.length > 0 && (
								<div className="vehicle-damages-list">
									{vehicleDamages.map((damage) => (
										<div className="vehicle-damage-card" key={damage.damageId ?? `${damage.contactZone}-${damage.direction}`}>
                                            <div>
                                                <div>
                                                    <strong>{damage.contactZone || 'Zona sem nome'}</strong>
                                                    <span>{damage.deformationType || 'Tipo não definido'}</span>
                                                </div>
                                                <button
                                                    className="remove-damage-btn"
                                                    type="button"
                                                    onClick={() => handleRemoveDamage(damage)}
                                                    disabled={!damage.damageId}
                                                >
                                                    Remover
                                                </button>
                                            </div>
											<p>{damage.damageDescription || 'Sem descrição.'}</p>
											<small>
												Direção: {damage.direction || '-'}
												{damage.heightCm !== null && damage.heightCm !== undefined
													? ` | Altura: ${damage.heightCm} cm`
													: ''}
											</small>
										</div>
									))}
								</div>
							)}
						</section>
					</div>
				</div>
			)}

			{selectedDamageVehicle && (
				<div className="vehicle-modal-backdrop" role="dialog" aria-modal="true">
					<div className="vehicle-modal">
						<h2>Criar dano</h2>
						<p>
							Registe um dano para {formatVehicleLabel(selectedDamageVehicle)}.
						</p>

						{damageError && <div className="vehicle-error-message">{damageError}</div>}

						<form onSubmit={handleDamageSubmit} className="vehicle-modal-form">
							<div className="vehicle-form-grid">
								<div className="vehicle-form-group">
									<label htmlFor="damage-contact-zone">Zona de contacto:</label>
									<input
										id="damage-contact-zone"
										value={damageInput.contactZone}
										onChange={(e) => handleDamageInputChange('contactZone', e.target.value)}
										placeholder="Ex.: Frente direita"
									/>
								</div>

								<div className="vehicle-form-group">
									<label htmlFor="damage-deformation-type">Tipo de deformação:</label>
									<input
										id="damage-deformation-type"
										value={damageInput.deformationType}
										onChange={(e) => handleDamageInputChange('deformationType', e.target.value)}
										placeholder="Ex.: Amolgadela"
									/>
								</div>

								<div className="vehicle-form-group wide">
									<label htmlFor="damage-direction">Direção:</label>
									<input
										id="damage-direction"
										value={damageInput.direction}
										onChange={(e) => handleDamageInputChange('direction', e.target.value)}
										placeholder="Ex.: Da frente para trás"
									/>
								</div>

								<div className="vehicle-form-group wide">
									<label htmlFor="damage-description">Descrição:</label>
									<textarea
										id="damage-description"
										value={damageInput.damageDescription}
										onChange={(e) => handleDamageInputChange('damageDescription', e.target.value)}
										placeholder="Descreva o dano observado"
										rows={4}
									/>
								</div>
							</div>

							<div className="vehicle-modal-actions">
								<button type="button" className="secondary-action" onClick={closeDamageModal}>
									Cancelar
								</button>
								<button type="submit" disabled={submittingDamage}>
									{submittingDamage ? 'A criar...' : 'Criar dano'}
								</button>
							</div>
						</form>
					</div>
				</div>
			)}

			{showVehicleModal && (
				<div className="vehicle-modal-backdrop" role="dialog" aria-modal="true">
					<div className="vehicle-modal">
						<h2>Adicionar veí­culo ao caso</h2>
						<p>Preencha os dados do veí­culo para o associar a este caso.</p>

						{vehicleError && <div className="vehicle-error-message">{vehicleError}</div>}

						<form onSubmit={handleVehicleSubmit} className="vehicle-modal-form">
							<div className="vehicle-form-grid">
								<div className="vehicle-form-group">
									<label htmlFor="case-vehicle-brand">Marca:</label>
									<input
										id="case-vehicle-brand"
										value={vehicleInput.brand}
										onChange={(e) => handleVehicleInputChange('brand', e.target.value)}
										placeholder="Ex.: Toyota"
									/>
								</div>

								<div className="vehicle-form-group">
									<label htmlFor="case-vehicle-model">Modelo:</label>
									<input
										id="case-vehicle-model"
										value={vehicleInput.model}
										onChange={(e) => handleVehicleInputChange('model', e.target.value)}
										placeholder="Ex.: Corolla"
									/>
								</div>

								<div className="vehicle-form-group">
									<label htmlFor="case-vehicle-year">Ano:</label>
									<input
										id="case-vehicle-year"
										type="number"
										min="1900"
										max={new Date().getFullYear() + 1}
										value={vehicleInput.yearOfFabrication}
										onChange={(e) => handleVehicleInputChange('yearOfFabrication', e.target.value)}
										placeholder="Ex.: 2020"
									/>
								</div>

								<div className="vehicle-form-group">
									<label htmlFor="case-vehicle-license">Matrícula:</label>
									<input
										id="case-vehicle-license"
										value={vehicleInput.licensePlate}
										onChange={(e) => handleVehicleInputChange('licensePlate', e.target.value)}
										placeholder="Ex.: AA-00-BB"
									/>
								</div>

								<div className="vehicle-form-group wide">
									<label htmlFor="case-vehicle-role">Papel no acidente:</label>
									<input
										id="case-vehicle-role"
										value={vehicleInput.role}
										onChange={(e) => handleVehicleInputChange('role', e.target.value)}
										placeholder="Ex.: Veiculo interveniente"
									/>
								</div>
							</div>

							<div className="vehicle-modal-actions">
								<button type="button" className="secondary-action" onClick={closeVehicleModal}>
									Cancelar
								</button>
								<button type="submit" disabled={submittingVehicle}>
									{submittingVehicle ? 'A adicionar...' : 'Adicionar veí­culo'}
								</button>
							</div>
						</form>
					</div>
				</div>
			)}
		</div>
	);
}

export default CaseInfoPage;
