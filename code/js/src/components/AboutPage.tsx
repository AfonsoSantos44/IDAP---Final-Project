import { Link } from 'react-router-dom';
import * as Icons from './utils/Icons';
import '../styles/AboutPage.css';
import { useAuth } from '../context/AuthContext';


const authors = [
    {
        name: "Afonso Santos",
        email: "A50484@alunos.isel.pt",
        githubLink: "https://github.com/AfonsoSantos44",
        image: "https://github.com/AfonsoSantos44.png"
    },
    {
        name: "Rodrigo Meneses",
        email: "A50542@alunos.isel.pt",
        githubLink: "https://github.com/rodrigombmeneses17",
        image: "https://github.com/rodrigombmeneses17.png"
    }
];

const techStack = [
    { name: 'Spring', color: 'highlight-emerald', stroke: '#34d399' },
    { name: 'Kotlin', color: 'highlight-purple', stroke: '#c084fc' },
    { name: 'React', color: 'highlight-cyan', stroke: '#22d3ee' },
    { name: 'Vite', color: 'highlight-amber', stroke: '#fbbf24' },
    { name: 'TypeScript', color: 'text-blue-400', stroke: '#60a5fa' },
];

export default function AboutPage() {
    const { token } = useAuth();

    const content = (
        <div className="about-wrapper">
            <div className="about-container">

                {/* Header */}
                <div className="about-header">
                    <div className="icon-badge">
                        <Icons.GraduationCap />
                    </div>
                    <h1 className="about-title">Sobre o projeto</h1>
                    <p className="about-subtitle">
                        Projeto desenvolvido para a cadeira de Projeto e Seminário no ISEL, com o objetivo de criar uma ferramenta de auxílio à comparação e análise de danos em sinistros automóveis.
                    </p>
                </div>

                {/* Project Info Card */}
                <div className="glass-card">
                    <div className="project-content">

                        <div>
                            <p className="project-text">
                                Este é um projeto <span className="highlight-amber">Spring + Kotlin / React + Vite + TypeScript </span>
                                desenvolvido no <span className="highlight-cyan">ISEL</span> com a orientação dos professores
                                <span className="highlight-purple"> Artur Ferreira</span> e
                                <span className="highlight-purple"> Pedro Miguens</span> na disciplina de
                                <span className="highlight-emerald"> Projeto e Seminário</span>.
                            </p>

                            <div className="tech-stack">
                                {techStack.map((tech) => (
                                    <div key={tech.name} className="tech-chip">
                                        <Icons.Code className={tech.color} />
                                        <span className={tech.color}>{tech.name}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Team Section */}
                <div>
                    <div className="section-title-container">
                        <Icons.Users />
                        <h2 className="about-title" style={{ fontSize: '1.5rem', margin: 0 }}>Grupo de Trabalho</h2>
                    </div>

                    <div className="team-grid">
                        {authors.map((author, index) => (
                            <div key={index} className="glass-card team-card">
                                <div className="avatar-wrapper">
                                    <img src={author.image} alt={author.name} className="avatar-img" />
                                </div>
                                <h3 className="member-name">{author.name}</h3>
                                <a href={`mailto:${author.email}`} className="member-email">
                                    <Icons.Mail />
                                    {author.email}
                                </a>
                                <a
                                    href={author.githubLink}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="github-btn"
                                >
                                    <Icons.Github />
                                    GitHub
                                </a>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Footer CTA */}
                <div className="about-footer">
                    <Link to="/" style={{ textDecoration: 'none' }}>
                        <button className="home-btn">
                            <Icons.Home />
                            Voltar à página inicial
                        </button>
                    </Link>
                </div>

            </div>
        </div>
    );

    return token ? <ProtectedLayout>{content}</ProtectedLayout> : content;
}