import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { Case } from '../../types/caseTypes';
import '../../styles/CaseInfoPage.css';
import { caseService } from '../../services/caseService';
import { userService } from '../../services/userService';

function CaseInfoPage() {
	const navigate = useNavigate();
	const { logout } = useAuth();

	const handleLogout = async () => {
		await logout();
		navigate('/initial');
	}
	const { id } = useParams<{ id: string }>();

	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [caseData, setCaseData] = useState<any | null>(null);
	const [reporterName, setReporterName] = useState<string | undefined>(undefined);

	useEffect(() => {
		const load = async () => {
			if (!id) return;
			setLoading(true);
			setError(null);
			try {
				const data = await caseService.getCase(Number(id));
				// data shape may vary; normalize
				const normalized = {
					id: data.id ?? data.caseId ?? id,
					description: data.description ?? data.accidentDescription ?? data.description,
					status: data.status ?? data.caseStatus ?? data.state,
					userId: data.userId ?? data.user ?? data.reporter,
					createdAt: data.createdAt ?? data.created_at ?? undefined,
					updatedAt: data.updatedAt ?? data.updated_at ?? undefined,
				};
				setCaseData(normalized);
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
	}, [id]);

	const caseEntries = caseData
		? Object.entries({
			Descrição: caseData.description,
			Estado: caseData.status,
			Averiguador: reporterName ?? caseData.userId ?? '-',
			Criado: caseData.createdAt,
			Atualizado: caseData.updatedAt,
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
			</section>

			<div className="case-info-bottom-action">
				<button
					className="page-btn"
					type="button"
					onClick={() => {
						navigate(`/cases/${caseData?.id ?? id}/menu`);
					}}
				>
					Ver Mais
				</button>
			</div>
		</div>
	);
}

export default CaseInfoPage;