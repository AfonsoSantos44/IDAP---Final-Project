import { API_BASE_URL, fetchApi } from './api';

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

export interface DamageSelectionRequest {
	x1: number;
	y1: number;
	x2: number;
	y2: number;
}

export interface RulerReferencePointRequest {
	x: number;
	y: number;
	valueCm: number;
}

export interface RulerCalibrationRequest {
	referencePoints: RulerReferencePointRequest[];
}

export interface CreateMeasurementRequest {
	evidenceId: number;
	damageId: number;
	comparisonEvidenceId: number;
	knownTickDistanceCm: number;
	primarySelection: DamageSelectionRequest;
	primaryCalibration?: RulerCalibrationRequest;
	comparisonSelection: DamageSelectionRequest;
	comparisonCalibration?: RulerCalibrationRequest;
}

export interface MeasurementOutput {
	measurementId?: number;
	analysisId?: number;
	evidenceId?: number;
	damageId?: number;
	calculatedHeightCm?: number;
	damageMinHeightCm?: number;
	damageMaxHeightCm?: number;
	scaleCmPerPixel?: number;
	confidence?: number;
	calibrationMethod?: string;
	comparisonImagePath?: string | null;
	processedAt?: string;
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

	async createMeasurement(
		analysisId: number,
		input: CreateMeasurementRequest
	): Promise<MeasurementOutput> {
		return fetchApi<MeasurementOutput>(`/analyses/${analysisId}/measurements`, {
			method: 'POST',
			body: JSON.stringify(input),
		});
	},

	comparisonImageContentUrl(measurementId: number): string {
		return `${API_BASE_URL}/measurements/${measurementId}/comparison-image`;
	},
};

export default accidentAnalysisService;
