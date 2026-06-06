import { createRoot } from "react-dom/client";
import { AuthProvider } from "./context/AuthContext";
import { createBrowserRouter, RouterProvider ,Navigate} from "react-router-dom"
import  HomePage  from "./components/HomePage";
import  LoginPage  from "./components/auth/LoginPage";
import  RegisterPage  from "./components/auth/RegisterPage";
import NotFoundPage from "./components/NotFoundPage";
import CasesListPage from "./components/cases/CasesListPage";
import CreateCasePage from "./components/cases/CreateCasePage";
import CaseInfoPage from "./components/cases/CaseInfoPage";
import AboutPage from "./components/AboutPage";

const router = createBrowserRouter([
    {
        path: "/",
        element: <Navigate to="/home" replace/>,
    },
    {
        path:"/home",
        element: <HomePage/>
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
        path: "*",
        element: <NotFoundPage/>,
    },
    
]);

createRoot(document.getElementById("container")!).render(
  <AuthProvider>
    <RouterProvider router={router} />
  </AuthProvider>
);