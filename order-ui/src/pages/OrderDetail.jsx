import { useEffect, useState } from 'react'

const API_URL = '/api/orders'

function Spinner() {
  return (
    <div className="flex items-center justify-center py-4">
      <svg
        className="animate-spin h-6 w-6 text-indigo-400"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
      </svg>
      <span className="ml-2 text-sm text-gray-400">Loading…</span>
    </div>
  )
}

function statusClass(status) {
  switch (status) {
    case 'PENDING':
      return 'bg-amber-500/20 text-amber-400 border-amber-500/30'
    case 'PROCESSED':
      return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
    default:
      return 'bg-gray-600/20 text-gray-400 border-gray-600/30'
  }
}

export default function OrderDetail({ id, onNavigateToCreate }) {
  const [order, setOrder] = useState(null)
  const [orderStatus, setOrderStatus] = useState(null)
  const [orderProcessedAt, setOrderProcessedAt] = useState(null)
  const [orderLoading, setOrderLoading] = useState(true)
  const [orderError, setOrderError] = useState(null)
  const [statusTimeout, setStatusTimeout] = useState(false)

  useEffect(() => {
    if (!id) return

    let cancelled = false
    let eventSource = null
    let timeoutTimer = null

    const loadOrder = async () => {
      try {
        setOrderLoading(true)
        setOrderError(null)
        const res = await fetch(`${API_URL}/${id}`)
        if (!res.ok) {
          throw new Error(`Failed to load order: ${res.status}`)
        }
        const data = await res.json()
        if (!cancelled) {
          setOrder(data)
          setOrderStatus(data.status)
          setOrderProcessedAt(data.processedAt)
          setOrderLoading(false)
        }
      } catch (err) {
        if (!cancelled) {
          setOrderError(err.message)
          setOrderLoading(false)
        }
      }
    }

    const startSSE = () => {
      eventSource = new EventSource(`${API_URL}/${id}/status/stream`)
      eventSource.addEventListener('order-status', (e) => {
        try {
          const data = JSON.parse(e.data)
          if (!cancelled) {
            setOrderStatus(data.status)
            setOrderProcessedAt(data.processedAt)
            if (data.status === 'PROCESSED') {
              eventSource.close()
              if (timeoutTimer) clearTimeout(timeoutTimer)
            }
          }
        } catch (err) {
          console.error('Failed to parse SSE data', err)
        }
      })
      eventSource.onerror = () => {
        eventSource.close()
      }
    }

    loadOrder()
    startSSE()

    timeoutTimer = setTimeout(() => {
      if (!cancelled) {
        setStatusTimeout(true)
        if (eventSource) eventSource.close()
      }
    }, 60000)

    return () => {
      cancelled = true
      if (eventSource) eventSource.close()
      if (timeoutTimer) clearTimeout(timeoutTimer)
    }
  }, [id])

  const goBack = () => {
    onNavigateToCreate()
  }

  if (orderLoading) {
    return (
      <div className="min-h-screen bg-gray-950 text-gray-100 flex items-center justify-center p-6">
        <Spinner />
      </div>
    )
  }

  if (orderError) {
    return (
      <div className="min-h-screen bg-gray-950 text-gray-100 flex items-center justify-center p-6">
        <div className="w-full max-w-md rounded-xl bg-gray-900 shadow-lg p-6">
          <div className="flex items-center gap-2 mb-6">
            <button onClick={goBack} className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-white transition-colors">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" /></svg>
              Back
            </button>
            <button onClick={onNavigateToCreate} className="inline-flex items-center gap-1.5 text-sm text-indigo-400 hover:text-white transition-colors">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
              Create New Order
            </button>
            <h1 className="text-2xl font-semibold text-white">Order #{id}</h1>
          </div>
          <div className="rounded-md bg-red-900/20 p-4 border border-red-500/30">
            <p className="text-sm text-red-400">Error: {orderError}</p>
            <button onClick={goBack} className="mt-3 inline-flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 underline">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" /></svg>
              Back to form
            </button>
          </div>
        </div>
      </div>
    )
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-gray-950 text-gray-100 flex items-center justify-center p-6">
        <div className="w-full max-w-md rounded-xl bg-gray-900 shadow-lg p-6 text-center py-8">
          <h1 className="text-2xl font-semibold mb-2 text-white">Order not found</h1>
          <button onClick={goBack} className="inline-flex items-center gap-1 text-sm text-indigo-400 hover:text-indigo-300 underline">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" /></svg>
            Back to form
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex items-center justify-center p-6">
      <div className="w-full max-w-md rounded-xl bg-gray-900 shadow-lg p-6">
        <div className="flex items-center gap-2 mb-6">
          <button onClick={goBack} className="inline-flex items-center gap-1.5 text-sm text-gray-400 hover:text-white transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" /></svg>
            Back
          </button>
          <button onClick={onNavigateToCreate} className="inline-flex items-center gap-1.5 text-sm text-indigo-400 hover:text-white transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
            Create New Order
          </button>
          <h1 className="text-2xl font-semibold text-white">Order #{id}</h1>
        </div>

        <div className="space-y-4 mb-6">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-gray-500 uppercase tracking-wider">Total</p>
              <p className="text-sm font-medium text-white">${Number(order.total).toFixed(2)}</p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase tracking-wider">Created By</p>
              <p className="text-sm font-medium text-white">{order.createdBy}</p>
            </div>
          </div>
          <div>
            <p className="text-xs text-gray-500 uppercase tracking-wider">Description</p>
            <p className="text-sm text-gray-300">{order.description}</p>
          </div>
          <div>
            <p className="text-xs text-gray-500 uppercase tracking-wider">Created At</p>
            <p className="text-sm text-gray-300">{new Date(order.createdAt).toLocaleString()}</p>
          </div>
        </div>

        <div className="rounded-lg px-4 py-3 border flex items-center gap-3">
          <span className="text-xs font-medium text-gray-300 uppercase tracking-wider">Status</span>
          {orderStatus === 'PENDING' && !statusTimeout && (
            <>
              <div className="flex items-center gap-1">
                <svg
                  className="animate-spin h-4 w-4 text-indigo-400"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                <span className="text-xs text-indigo-400 font-medium">Processing…</span>
              </div>
            </>
          )}
          {statusTimeout && orderStatus === 'PENDING' && (
            <>
              <svg
                className="h-4 w-4 text-amber-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
              <span className="text-xs text-amber-500 font-medium">Timed out</span>
            </>
          )}
          {orderStatus !== 'PENDING' && (
            <>
              <span className={`px-2.5 py-0.5 rounded text-xs font-semibold ${statusClass(orderStatus)}`}>{orderStatus}</span>
              {orderStatus === 'PROCESSED' && orderProcessedAt && (
                <>
                  <span className="text-xs text-gray-500">•</span>
                  <span className="text-xs text-emerald-400">Processed at {new Date(orderProcessedAt).toLocaleString()}</span>
                </>
              )}
            </>
          )}
        </div>

        {statusTimeout && (
          <div className="mt-4 rounded-md bg-amber-900/20 p-4 border border-amber-500/30">
            <p className="text-sm text-amber-400">Status did not reach PROCESSED within 60 seconds.</p>
          </div>
        )}

        {orderStatus === 'PROCESSED' && (
          <div className="mt-4 rounded-md bg-emerald-900/20 p-4 border border-emerald-500/30">
            <p className="text-sm text-emerald-400">Order has been processed successfully.</p>
          </div>
        )}
      </div>
    </div>
  )
}
