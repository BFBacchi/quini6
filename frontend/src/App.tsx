import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Dashboard } from './components/Dashboard/Dashboard';
import { Heatmap } from './components/Heatmap/Heatmap';
import { GeneradorCombinaciones } from './components/GeneradorCombinaciones/GeneradorCombinaciones';
import { BuscadorSorteos } from './components/BuscadorSorteos/BuscadorSorteos';
import { Monitoring } from './components/Monitoring/Monitoring';
import { DisclaimerBanner } from './components/common/DisclaimerBanner';
import { Navbar } from './components/common/Navbar';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50">
        <DisclaimerBanner />
        <Navbar />
        <main className="container mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/frecuencia" element={<Heatmap />} />
            <Route path="/generador" element={<GeneradorCombinaciones />} />
            <Route path="/sorteos" element={<BuscadorSorteos />} />
            <Route path="/monitoring" element={<Monitoring />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
