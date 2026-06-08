import { fetchApi } from "./api";
import { CaseDetailsOutput,CreateCaseInput, Case } from "../types/caseTypes";

export const caseService = {
    async createCase(input: CreateCaseInput):Promise<CaseDetailsOutput>{
        return fetchApi<CaseDetailsOutput>("/cases",{
            method: "POST",
            body: JSON.stringify(input),
        });
    },

    async getAllCases():Promise<Case[]>{
        return fetchApi<Case[]>('/cases', {
            method: 'GET',
        });
    },

    async getCase(id: number): Promise<CaseDetailsOutput>{
        return fetchApi<CaseDetailsOutput>(`/cases/${id}`,{
            method: "GET",
        })
    }

}