import { fetchApi, uploadApi, API_BASE_URL } from './api';

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
	vehicleId?: number;
	filePath?: string;
	width?: number;
	height?: number;
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

	// Uploads the actual image bytes as multipart/form-data; stored in object storage (MinIO).
	async uploadEvidenceImage(
		evidenceId: number,
		file: File,
		vehicleId: number,
		metadata?: string,
	): Promise<ImageEvidenceOutput> {
		const form = new FormData();
		form.append('file', file);
		form.append('vehicleId', String(vehicleId));
		if (metadata) form.append('metadata', metadata);
		return uploadApi<ImageEvidenceOutput>(`/evidence/${evidenceId}/image`, form, 'PUT');
	},

	// Direct URL to the raw image bytes, usable as an <img src>. The cache-busting `v`
	// param forces the browser to refetch after a new upload replaces the image.
	evidenceImageContentUrl(evidenceId: number, version?: number): string {
		const suffix = version !== undefined ? `?v=${version}` : '';
		return `${API_BASE_URL}/evidence/${evidenceId}/image/content${suffix}`;
	},

	async deleteEvidenceImage(evidenceId: number): Promise<void> {
		return fetchApi<void>(`/evidence/${evidenceId}/image`, {
			method: 'DELETE',
		});
	},
};

export default evidenceService;

