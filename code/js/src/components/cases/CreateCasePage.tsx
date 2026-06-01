import { CreateCaseInput } from '../../types/caseTypes';
import '../../styles/CreateCasePage.css';

import React, { useState } from 'react';

function CreateCasePage() {
    const [input, setInput] = useState<CreateCaseInput>({
        user: 0,
        description: '',
        status: '',
    });

    const handleInputChange = (field: keyof CreateCaseInput, value: any) => {
        setInput((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSubmit = () => {
        console.log('Submitting case:', input);
    };

    return (
        <div className="create-case-page">
            <button onClick={() => { window.location.href = '/cases'; }}>Voltar</button>
            <h1>Criar um novo caso</h1>
            <form onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
                <div className="form-group">
                    <label htmlFor="user">Averiguador:</label>
                    <input
                        type="number"
                        id="user"
                        placeholder="Enter user ID"
                        value={input.user}
                        onChange={(e) => handleInputChange('user', parseInt(e.target.value))}
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="description">Descrição:</label>
                    <textarea
                        id="description"
                        placeholder="Enter case description"
                        value={input.description}
                        onChange={(e) => handleInputChange('description', e.target.value)}
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="status">Estado:</label>
                    <input
                        type="text"
                        id="status"
                        placeholder="Enter case status"
                        value={input.status}
                        onChange={(e) => handleInputChange('status', e.target.value)}
                    />
                </div>

                <button type="submit">Criar Caso</button>
            </form>
        </div>
    );
};

export default CreateCasePage;
