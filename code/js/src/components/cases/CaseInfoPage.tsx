import React from 'react';
import type { Case } from '../../types/caseTypes';
import '../../styles/CaseInfoPage.css';

function CaseInfoPage() {
	// add api request to fetch case details by ID

	const cases = {
		id: '1',
		Título: 'Caso de teste 1',
		Descrição: 'Descrição do caso de teste 1',
		Estado: 'Aberto',
		Averiguador: 'Averiguador 1',
		Criado: new Date().toISOString(),
		Atualizado: new Date().toISOString()
	};

	const caseEntries = Object.entries(cases) as [string, unknown][];

	return (
		<div className="case-info-page">
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
				{caseEntries.map(([key, value]) => (
					<div className="case-info-row" key={key}>
						<strong className="case-info-label">
							{key}:
						</strong>

						<span
							className={
								key === 'Estado'
									? `case-status ${String(value)}`
									: 'case-info-value'
							}
						>
							{value === null || value === undefined
								? '-'
								: Array.isArray(value)
								? value.join(', ')
								: typeof value === 'object'
								? JSON.stringify(value, null, 2)
								: String(value)}
						</span>
					</div>
				))}
			</section>

			<div className="case-info-bottom-action">
				<button
					className="page-btn"
					type="button"
					onClick={() => {
						window.location.href = `/analysis/${cases.id}`;
					}}
				>
					Analisar
				</button>
			</div>
		</div>
	);
}

export default CaseInfoPage;