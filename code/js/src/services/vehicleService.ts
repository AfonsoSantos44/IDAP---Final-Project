import { fetchApi } from './api';

export interface VehicleOutput {
  vehicleId?: number;
  caseId?: number;
  brand?: string;
  model?: string;
  yearOfFabrication?: number;
  licensePlate?: string;
  role?: string | null;
}

export interface CreateVehicleRequest {
  brand: string;
  model: string;
  yearOfFabrication: number;
  licensePlate: string;
  role?: string | null;
}

export const vehicleService = {
  async getCaseVehicles(caseId: number): Promise<VehicleOutput[]> {
    return fetchApi<VehicleOutput[]>(`/cases/${caseId}/vehicles`);
  },

  async createCaseVehicle(caseId: number, input: CreateVehicleRequest): Promise<VehicleOutput> {
    return fetchApi<VehicleOutput>(`/cases/${caseId}/vehicles`, {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  async deleteVehicle(vehicleId: number): Promise<void> {
    return fetchApi<void>(`/vehicles/${vehicleId}`, {
      method: 'DELETE',
    });
  },
};

export default vehicleService;
