import { useState } from 'react'

const API_URL = '/api/orders'

export default function CreateOrder({ onOrderCreated }) {
  const [total, setTotal] = useState('')
  const [description, setDescription] = useState('')
  const [createdBy, setCreatedBy] = useState('eduardo')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          total: Number(total),
          description,
          createdBy: createdBy || null
        })
      })
      if (!res.ok) {
        throw new Error(`Request failed: ${res.status}`)
      }
      const data = await res.json()
      onOrderCreated(data.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const inputClass =
    'mt-1 w-full rounded-md border border-gray-700 bg-gray-800 px-3 py-2 text-gray-100 placeholder-gray-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500'

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex items-center justify-center p-6">
      <div className="w-full max-w-md rounded-xl bg-gray-900 shadow-lg p-6">
        <h1 className="text-2xl font-semibold mb-6 text-white">Create Order</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-300">Total</label>
            <input
              type="number"
              step="0.01"
              required
              value={total}
              onChange={(e) => setTotal(e.target.value)}
              className={inputClass}
              placeholder="0.00"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-300">Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className={inputClass}
              placeholder="Order description"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-300">Created By</label>
            <select
              value={createdBy}
              onChange={(e) => setCreatedBy(e.target.value)}
              className={inputClass}
            >
              <option value="eduardo">eduardo</option>
              <option value="alice">alice</option>
              <option value="tim">tim</option>
              <option value="jim">jim</option>
              <option value="martha">martha</option>
            </select>
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Creating...' : 'Create Order'}
          </button>
        </form>
        {error && (
          <p className="mt-4 text-sm text-red-400">Error: {error}</p>
        )}
      </div>
    </div>
  )
}
