import { fetchApi } from './api';

export interface EvidenceOutput {
	evidenceId?: number;
	caseId?: number;
	evidenceType?: string;
	evidenceDescription?: string;
	uploadedBy?: number;
	uploadedAt?: string;
}

export interface CreateEvidenceRequest {
	evidenceType: string;
	evidenceDescription: string;
}

export interface UpdateEvidenceRequest {
	evidenceType?: string;
	evidenceDescription?: string;
}

export interface ImageEvidenceOutput {
	imageEvidenceId?: number;
	evidenceId?: number;
	filePath?: string;
	width?: number;
	height?: number;
	metadata?: string | null;
}

export interface UpsertImageEvidenceRequest {
	filePath: string;
	width: number;
	height: number;
	metadata?: string | null;
}

export const evidenceService = {
	// Evidence listing and creation under a case
	async listCaseEvidence(caseId: number): Promise<EvidenceOutput[]> {
		return fetchApi<EvidenceOutput[]>(`/cases/${caseId}/evidence`, {
			method: 'GET',
		});
	},

	async createCaseEvidence(caseId: number, input: CreateEvidenceRequest): Promise<EvidenceOutput> {
		return fetchApi<EvidenceOutput>(`/cases/${caseId}/evidence`, {
			method: 'POST',
			body: JSON.stringify(input),
		});
	},

	// Evidence by id
	async getEvidenceById(evidenceId: number): Promise<EvidenceOutput> {
		return fetchApi<EvidenceOutput>(`/evidence/${evidenceId}`, {
			method: 'GET',
		});
	},

	async updateEvidence(evidenceId: number, input: UpdateEvidenceRequest): Promise<EvidenceOutput> {
		return fetchApi<EvidenceOutput>(`/evidence/${evidenceId}`, {
			method: 'PUT',
			body: JSON.stringify(input),
		});
	},

	async deleteEvidence(evidenceId: number): Promise<void> {
		return fetchApi<void>(`/evidence/${evidenceId}`, {
			method: 'DELETE',
		});
	},

	// Image metadata endpoints for evidence
	async getEvidenceImage(evidenceId: number): Promise<ImageEvidenceOutput> {
		return fetchApi<ImageEvidenceOutput>(`/evidence/${evidenceId}/image`, {
			method: 'GET',
		});
	},

	async upsertEvidenceImage(evidenceId: number, input: UpsertImageEvidenceRequest): Promise<ImageEvidenceOutput> {
		return fetchApi<ImageEvidenceOutput>(`/evidence/${evidenceId}/image`, {
			method: 'PUT',
			body: JSON.stringify(input),
		});
	},

	async deleteEvidenceImage(evidenceId: number): Promise<void> {
		return fetchApi<void>(`/evidence/${evidenceId}/image`, {
			method: 'DELETE',
		});
	},
};

export default evidenceService;

