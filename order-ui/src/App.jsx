import { useState, useEffect } from 'react'
import CreateOrder from './pages/CreateOrder'
import OrderDetail from './pages/OrderDetail'

const ROUTES = {
  CREATE: 'create',
  DETAIL: 'detail',
}

function App() {
  const [route, setRoute] = useState(ROUTES.CREATE)
  const [orderId, setOrderId] = useState(null)

  const navigateToCreate = () => {
    setRoute(ROUTES.CREATE)
    setOrderId(null)
    window.history.pushState({}, '', '/orders/create')
  }

  const navigateToDetail = (id) => {
    setRoute(ROUTES.DETAIL)
    setOrderId(id)
    window.history.pushState({}, '', `/orders/${id}`)
  }

  const handlePopState = () => {
    const path = window.location.pathname
    if (path === '/' || path === '/orders/create') {
      navigateToCreate()
    } else if (path.startsWith('/orders/')) {
      const id = path.split('/').pop()
      navigateToDetail(id)
    }
  }

  const initRoute = () => {
    const path = window.location.pathname
    if (path === '/' || path === '/orders/create') {
      navigateToCreate()
    } else if (path.startsWith('/orders/')) {
      const id = path.split('/').pop()
      if (id && id !== 'create') {
        navigateToDetail(id)
      } else {
        navigateToCreate()
      }
    }
  }

  const handleLinkClick = (e) => {
    const link = e.target.closest('a')
    if (!link) return
    const href = link.getAttribute('href')
    if (!href || href.startsWith('http') || href.startsWith('#')) return
    e.preventDefault()
    if (href === '/' || href === '/orders/create') {
      navigateToCreate()
    } else if (href.startsWith('/orders/')) {
      const id = href.split('/').pop()
      if (id && id !== 'create') {
        navigateToDetail(id)
      } else {
        navigateToCreate()
      }
    }
  }

  const handleOrderCreated = (id) => {
    navigateToDetail(id)
  }

  useEffect(() => {
    initRoute()
    window.addEventListener('popstate', handlePopState)
    return () => {
      window.removeEventListener('popstate', handlePopState)
    }
  }, [])

  return (
    <div onClick={handleLinkClick} className="min-h-screen bg-gray-950 text-gray-100">
      {route === ROUTES.CREATE && (
        <CreateOrder onOrderCreated={handleOrderCreated} />
      )}
      {route === ROUTES.DETAIL && orderId && (
        <OrderDetail id={orderId} onNavigateToCreate={navigateToCreate} />
      )}
    </div>
  )
}

export default App
