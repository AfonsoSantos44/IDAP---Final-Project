import { fetchApi } from './api';

export interface AccidentAnalysisOutput {
	analysisId?: number;
	caseId?: number;
	createdBy?: number;
	createdAt?: string;
}

export interface AnalysisImageOutput {
	analysisId?: number;
	evidenceId?: number;
	purpose?: string;
}

export interface UpsertAnalysisImageRequest {
	evidenceId: number;
	purpose: string;
}

export const accidentAnalysisService = {
	// Analyses under a case

	async listCaseAnalyses(caseId: number): Promise<AccidentAnalysisOutput[]> {
		return fetchApi<AccidentAnalysisOutput[]>(`/cases/${caseId}/analyses`, {
			method: 'GET',
		});
	},

	async createCaseAnalysis(caseId: number): Promise<AccidentAnalysisOutput> {
		return fetchApi<AccidentAnalysisOutput>(`/cases/${caseId}/analyses`, {
			method: 'POST',
		});
	},

	// Analysis by id

	async getAnalysisById(analysisId: number): Promise<AccidentAnalysisOutput> {
		return fetchApi<AccidentAnalysisOutput>(`/analyses/${analysisId}`, {
			method: 'GET',
		});
	},

	async deleteAnalysis(analysisId: number): Promise<void> {
		return fetchApi<void>(`/analyses/${analysisId}`, {
			method: 'DELETE',
		});
	},

	// Analysis images

	async getAnalysisImages(analysisId: number): Promise<AnalysisImageOutput[]> {
		return fetchApi<AnalysisImageOutput[]>(`/analyses/${analysisId}/images`, {
			method: 'GET',
		});
	},

	async upsertAnalysisImage(
		analysisId: number,
		input: UpsertAnalysisImageRequest
	): Promise<AnalysisImageOutput> {
		return fetchApi<AnalysisImageOutput>(`/analyses/${analysisId}/images`, {
			method: 'PUT',
			body: JSON.stringify(input),
		});
	},

	async deleteAnalysisImage(
		analysisId: number,
		evidenceId: number
	): Promise<void> {
		return fetchApi<void>(
			`/analyses/${analysisId}/images/${evidenceId}`,
			{
				method: 'DELETE',
			}
		);
	},
};

export default accidentAnalysisService;