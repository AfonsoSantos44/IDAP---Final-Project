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

export interface DamageOutput {
  damageId?: number;
  vehicleId?: number;
  contactZone?: string;
  deformationType?: string;
  direction?: string;
  heightCm?: number | null;
  damageDescription?: string;
}

export interface CreateDamageRequest {
  contactZone: string;
  deformationType: string;
  direction: string;
  damageDescription: string;
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

  async createVehicleDamage(vehicleId: number, input: CreateDamageRequest): Promise<DamageOutput> {
    return fetchApi<DamageOutput>(`/vehicles/${vehicleId}/damages`, {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  async getVehicleDamages(vehicleId: number): Promise<DamageOutput[]> {
    return fetchApi<DamageOutput[]>(`/vehicles/${vehicleId}/damages`);
  },

  async deleteDamage(damageId: number): Promise<void> {
    return fetchApi<void>(`/damages/${damageId}`, {
      method: 'DELETE',
    });
  },
};

export default vehicleService;
