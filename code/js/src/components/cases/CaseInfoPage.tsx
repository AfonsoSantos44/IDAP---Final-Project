import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/CaseInfoPage.css';
import { caseService } from '../../services/caseService';
import { userService } from '../../services/userService';
import analysisService from '../../services/analysisService';
import { vehicleService, type CreateVehicleRequest, type VehicleOutput } from '../../services/vehicleService';

type VehicleFormInput = {
	brand: string;
	model: string;
	yearOfFabrication: string;
	licensePlate: string;
	role: string;
};

const emptyVehicleForm: VehicleFormInput = {
	brand: '',
	model: '',
	yearOfFabrication: '',
	licensePlate: '',
	role: '',
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
		if (!vehicleInput.brand.trim()) return 'A marca do veiculo é obrigatória.';
		if (!vehicleInput.model.trim()) return 'O modelo do veiculo é obrigatório.';
		if (!vehicleInput.licensePlate.trim()) return 'A matricula do veiculo é obrigatória.';

		const year = Number(vehicleInput.yearOfFabrication);
		if (!Number.isInteger(year)) return 'O ano de fabricação deve ser um número inteiro.';
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
			setVehicleError('Não foi possível associar o veículo ao caso.');
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
			setVehicleError(err?.message || 'Falha ao adicionar veículo.');
		} finally {
			setSubmittingVehicle(false);
		}
	};

	const handleRemoveVehicle = async (vehicle: VehicleOutput) => {
		if (!vehicle.vehicleId) return;
		if (!confirm('Tem a certeza que deseja remover este veículo?')) return;

		try {
			await vehicleService.deleteVehicle(vehicle.vehicleId);
			setVehicles((prev) => prev.filter((item) => item.vehicleId !== vehicle.vehicleId));
		} catch (err: any) {
			alert(err?.message || 'Falha ao remover veículo.');
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
			alert(err?.message || 'Erro ao criar análise');
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
								Veículos envolvidos:
							</strong>
							<button className="add-case-vehicle-btn" type="button" onClick={openVehicleModal}>
								Adicionar veículo
							</button>
						</div>

						<div className={`case-vehicles-list ${vehicles.length > 4 ? 'scrollable' : ''}`}>
							{vehicles.length === 0 ? (
								<span className="case-info-value">Ainda não existem veículos adicionados.</span>
							) : (
								vehicles.map((vehicle) => (
									<div className="case-vehicle-card" key={vehicle.vehicleId ?? `${vehicle.licensePlate}-${vehicle.model}`}>
										<div className="case-vehicle-details">
											<strong>{formatVehicleLabel(vehicle)}</strong>
											<span>{vehicle.licensePlate ?? 'Sem matrícula'} - {vehicle.yearOfFabrication ?? 'Ano desconhecido'}</span>
											{vehicle.role && <small>{vehicle.role}</small>}
										</div>
										<button
											className="remove-vehicle-btn"
											type="button"
											onClick={() => handleRemoveVehicle(vehicle)}
											disabled={!vehicle.vehicleId}
										>
											Remover
										</button>
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
					Ver todas as evidências
				</button>

				<button
					className="case-menu-btn"
					type="button"
					onClick={() => {
						navigate(`/cases/${caseData?.id ?? currentCaseId}/evidences/create`);
					}}
				>
					Adicionar evidência
				</button>

				<button
					className="case-menu-btn"
					type="button"
					onClick={handleAnalyze}
				>
				Analisar
				</button>
			</div>

			{showVehicleModal && (
				<div className="vehicle-modal-backdrop" role="dialog" aria-modal="true">
					<div className="vehicle-modal">
						<h2>Adicionar veículo ao caso</h2>
						<p>Preencha os dados do veículo para o associar a este caso.</p>

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
									{submittingVehicle ? 'A adicionar...' : 'Adicionar veículo'}
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
