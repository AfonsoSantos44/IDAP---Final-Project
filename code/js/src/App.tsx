import { createRoot } from "react-dom/client";
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
import EvidenceMenu from "./components/evidences/EvidenceMenu";
import EvidenceList from "./components/evidences/EvidenceList";
import EvidenceCreate from "./components/evidences/EvidenceCreate";
import ReportCreate from "./components/evidences/AnalysisCreate";
import EvidenceUpdate from "./components/evidences/EvidenceUpdate";
import AnalysisCreate from "./components/evidences/AnalysisCreate";

const router = createBrowserRouter([
    {
        path: "/",
        element: <Navigate to="/home" replace/>,
    },
    {
        path:"/initial",
        element: <InitialPage/>
    },
    {
        path:"/home",
        element: <HomePage/>
    },
    {
        path: "/profile",
        element: <ProfilePage/>,
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
        element: <CasesListPage/>,
    },
    {
        path: "/cases/create",
        element: <CreateCasePage/>,
    },
    {
        path: "/cases/:id",
        element: <CaseInfoPage/>,
    },
    {
        path: "/about",
        element:<AboutPage/>,
    },
    {
        path: "/cases/:caseId/evidences",
        element: <EvidenceList/>,
    },
    {
        path: "/cases/:caseId/evidences/create",
        element: <EvidenceCreate/>,
    },
    {
        path: "/cases/:caseId/evidences/:evidenceId/edit",
        element: <EvidenceUpdate/>,
    },
    {
        path: "/cases/:caseId/menu",
        element: <EvidenceMenu/>,
    },
    {
        path: "/cases/:caseId/reports/create",
        element: <ReportCreate/>,
    },
    {
        path: "/cases/:caseId/analysis/:analysisId",
        element: <AnalysisCreate/>,
    },
    {
        path: "*",
        element: <NotFoundPage/>,
    },
    
]);

createRoot(document.getElementById("container")!).render(
  <AuthProvider>
    <RouterProvider router={router} />
  </AuthProvider>
);