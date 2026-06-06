
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
    id: number,
    user: number,
    createdAt: Date,
    status: string,
    description: string,
}