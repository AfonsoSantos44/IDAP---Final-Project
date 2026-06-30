
export interface Case{
    id: number,
    user: number,
    createdAt: Date
    status: string,
    description: string,
}


export interface CreateCaseInput{
    user: number,
    description: string,
    status: string,
};

export interface CaseDetailsOutput{
    caseId?: number,
    id: number,
    userId?: number,
    user: number,
    createdAt: Date,
    status: string,
    description: string,
}
