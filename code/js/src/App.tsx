import { createRoot } from "react-dom/client";
import type React from "react";
import { AuthProvider } from "./context/AuthContext";
import { createBrowserRouter, RouterProvider ,Navigate} from "react-router-dom"
import  InitialPage  from "./components/InitialPage";
import  LoginPage  from "./components/auth/LoginPage";
import  RegisterPage  from "./components/auth/RegisterPage";
import NotFoundPage from "./components/NotFoundPage";
import CasesListPage from "./components/cases/CasesListPage";
import CreateCasePage from "./components/cases/CreateCasePage";
import CaseInfoPage from "./components/cases/CaseInfoPage";
import AboutPage from "./components/AboutPage";
import HomePage from "./components/HomePage";
import ProfilePage from "./components/auth/ProfilePage";
import EvidenceList from "./components/evidences/EvidenceList";
import EvidenceCreate from "./components/evidences/EvidenceCreate";
import EvidenceUpdate from "./components/evidences/EvidenceUpdate";
import AnalysisCreate from "./components/analysis/AnalysisCreate";
import AnalysisImageCompare from "./components/analysis/AnalysisImageCompare";
import ReportCreate from "./components/analysis/ReportCreate";
import ReportList from "./components/analysis/ReportList";
import ReportDetail from "./components/analysis/ReportDetail";
import { ProtectedRoute } from "./components/ProtectedRoute";

const protect = (element: React.ReactNode) => (
    <ProtectedRoute>{element}</ProtectedRoute>
);

const router = createBrowserRouter([
    {
        path: "/",
        element: protect(<Navigate to="/home" replace/>),
    },
    {
        path:"/initial",
        element: <InitialPage/>
    },
    {
        path:"/home",
        element: protect(<HomePage/>)
    },
    {
        path: "/profile",
        element: protect(<ProfilePage/>),
    },
   {
        path: "/login",
        element: <LoginPage/>,
    },
    
    {
        path: "/register",
        element: <RegisterPage/>,
    },
    {
        path: "/cases",
        element: protect(<CasesListPage/>),
    },
    {
        path: "/cases/create",
        element: protect(<CreateCasePage/>),
    },
    {
        path: "/cases/:id",
        element: protect(<CaseInfoPage/>),
    },
    {
        path: "/about",
        element: protect(<AboutPage/>),
    },
    {
        path: "/cases/:caseId/evidences",
        element: protect(<EvidenceList/>),
    },
    {
        path: "/cases/:caseId/evidences/create",
        element: protect(<EvidenceCreate/>),
    },
    {
        path: "/cases/:caseId/evidences/:evidenceId/edit",
        element: protect(<EvidenceUpdate/>),
    },
    {
        path: "/cases/:caseId/analysis/image",
        element: protect(<AnalysisImageCompare/>),
    },
    {
        path: "/cases/:caseId/analysis/:analysisId/report",
        element: protect(<ReportCreate/>),
    },
    {
        path: "/reports",
        element: protect(<ReportList/>),
    },
    {
        path: "/reports/:reportId",
        element: protect(<ReportDetail/>),
    },
    {
        path: "/cases/:caseId/analysis/:analysisId",
        element: protect(<AnalysisCreate/>),
    },
    {
        path: "*",
        element: protect(<NotFoundPage/>),
    },
    
]);

createRoot(document.getElementById("container")!).render(
  <AuthProvider>
    <RouterProvider router={router} />
  </AuthProvider>
);
