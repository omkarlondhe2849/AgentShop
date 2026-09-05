import { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Search, ShoppingCart, Star, Filter, ChevronDown, Zap, SlidersHorizontal,
  ArrowUpDown, Package, Grid3X3, Sparkles, Heart, Eye, X, Check
} from 'lucide-react'
import { getCatalog } from '../services/api'
import { toast } from 'sonner'
import { useNavigate } from 'react-router-dom'

const CATEGORIES = ['All', 'Laptops', 'Smartphones', 'Headphones', 'Smartwatches', 'Tablets', 'Cameras']
const SORT_OPTIONS = [
  { label: 'Popularity', value: 'popularity' },
  { label: 'Price: Low to High', value: 'price-asc' },
  { label: 'Price: High to Low', value: 'price-desc' },
  { label: 'Rating', value: 'rating' },
]

export default function StorefrontPage() {
  const [products, setProducts] = useState([])
  const [filteredProducts, setFilteredProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [sortBy, setSortBy] = useState('popularity')
  const [priceRange, setPriceRange] = useState([0, 200000])
  const [showSortMenu, setShowSortMenu] = useState(false)
  const [wishlist, setWishlist] = useState(new Set())
  const [selectedProduct, setSelectedProduct] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    fetchProducts()
  }, [])

  const fetchProducts = async () => {
    try {
      const data = await getCatalog()
      const items = data.itemListElement || data || []
      setProducts(items)
      setFilteredProducts(items)
    } catch (error) {
      console.error('Failed to fetch catalog:', error)
      toast.error('Failed to load products')
    } finally {
      setLoading(false)
    }
  }

  // Filter and sort logic
  useEffect(() => {
    let result = [...products]

    // Category filter
    if (selectedCategory !== 'All') {
      result = result.filter(p => p.category === selectedCategory)
    }

    // Search filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase()
      result = result.filter(p =>
        p.name?.toLowerCase().includes(q) ||
        p.description?.toLowerCase().includes(q) ||
        p.tags?.toLowerCase().includes(q) ||
        p.category?.toLowerCase().includes(q)
      )
    }

    // Price range filter
    result = result.filter(p => {
      const price = parseFloat(p.offers?.price) || 0
      return price >= priceRange[0] && price <= priceRange[1]
    })

    // Sort
    switch (sortBy) {
      case 'price-asc':
        result.sort((a, b) => (parseFloat(a.offers?.price) || 0) - (parseFloat(b.offers?.price) || 0))
        break
      case 'price-desc':
        result.sort((a, b) => (parseFloat(b.offers?.price) || 0) - (parseFloat(a.offers?.price) || 0))
        break
      case 'rating':
        result.sort((a, b) => (b.aggregateRating?.ratingValue || 0) - (a.aggregateRating?.ratingValue || 0))
        break
      case 'popularity':
      default:
        result.sort((a, b) => (b.aggregateRating?.reviewCount || 0) - (a.aggregateRating?.reviewCount || 0))
        break
    }

    setFilteredProducts(result)
  }, [products, selectedCategory, searchQuery, sortBy, priceRange])

  const handleAddToCart = async (productId) => {
    try {
      const sessionId = localStorage.getItem('agentShopSessionId') || 'default-session'
      const res = await fetch(`/api/v1/cart`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId, productId, quantity: 1 })
      })
      if (res.ok) {
        toast.success('Added to cart! 🛒')
      } else {
        toast.error('Failed to add to cart')
      }
    } catch (err) {
      toast.error('Network error')
    }
  }

  const handleAskAI = (productName) => {
    navigate(`/chat?query=Tell me about ${productName}`)
  }

  const toggleWishlist = (id) => {
    setWishlist(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(price)
  }

  const getCategoryIcon = (cat) => {
    switch(cat) {
      case 'Laptops': return '💻'
      case 'Smartphones': return '📱'
      case 'Headphones': return '🎧'
      case 'Smartwatches': return '⌚'
      case 'Tablets': return '📱'
      case 'Cameras': return '📸'
      default: return '📦'
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 'calc(100vh - 65px)', color: 'var(--text-primary)' }}>
        <div style={{ textAlign: 'center' }}>
          <Package className="spin-slow" size={40} />
          <p style={{ marginTop: '1rem', color: 'var(--text-secondary)' }}>Loading products...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="storefront-page" style={{ background: 'var(--bg-primary)', color: 'var(--text-primary)', minHeight: 'calc(100vh - 65px)' }}>

      {/* Hero Banner */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        style={{
          background: 'var(--gradient-store-hero)',
          borderRadius: '20px',
          padding: '3rem',
          marginBottom: '2rem',
          position: 'relative',
          overflow: 'hidden',
          boxShadow: '0 20px 40px rgba(0,0,0,0.15)'
        }}
      >
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, background: 'radial-gradient(circle at 80% 20%, rgba(82, 143, 240, 0.3) 0%, transparent 60%)' }} />
        <div style={{ position: 'relative', zIndex: 1, color: '#fff' }}>
          <h1 style={{ fontSize: '2.5rem', fontWeight: 900, marginBottom: '0.5rem', color: '#fff' }}>
            <Sparkles size={32} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '10px' }} />
            Discover Premium Tech
          </h1>
          <p style={{ fontSize: '1.1rem', maxWidth: '600px', marginBottom: '1.5rem', fontWeight: 500, color: 'rgba(255,255,255,0.8)' }}>
            Browse {products.length}+ curated products. Add to cart instantly, or let our AI agent find exactly what you need.
          </p>
          <button
            className="btn"
            onClick={() => navigate('/chat')}
            style={{ 
              background: '#fff', color: '#072654', padding: '12px 28px', 
              borderRadius: '12px', fontWeight: 700, cursor: 'pointer', 
              display: 'inline-flex', alignItems: 'center', gap: '8px',
              transition: 'all 0.2s', boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
            }}
          >
            <Zap size={18} /> Shop with AI Assistant
          </button>
        </div>
      </motion.div>

      {/* Search & Filter Bar */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '1.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
        <div style={{ flex: 1, minWidth: '250px', position: 'relative' }}>
          <Search size={18} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Search products..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%',
              padding: '12px 14px 12px 42px',
              borderRadius: '12px',
              border: '1px solid var(--border-color)',
              background: 'var(--bg-secondary)',
              color: 'var(--text-primary)',
              fontSize: '0.95rem',
              outline: 'none',
              transition: 'border-color 0.2s',
              fontFamily: 'var(--font-body)'
            }}
          />
        </div>

        {/* Sort dropdown */}
        <div style={{ position: 'relative' }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setShowSortMenu(!showSortMenu)}
            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '12px 16px' }}
          >
            <ArrowUpDown size={14} />
            {SORT_OPTIONS.find(s => s.value === sortBy)?.label}
            <ChevronDown size={14} />
          </button>
          {showSortMenu && (
            <div style={{
              position: 'absolute', top: '100%', right: 0, marginTop: '4px',
              background: 'var(--bg-secondary)', border: '1px solid var(--border-color)',
              borderRadius: '12px', padding: '6px', zIndex: 50, minWidth: '180px',
              boxShadow: '0 8px 24px rgba(0,0,0,0.15)'
            }}>
              {SORT_OPTIONS.map(opt => (
                <button
                  key={opt.value}
                  onClick={() => { setSortBy(opt.value); setShowSortMenu(false) }}
                  style={{
                    display: 'block', width: '100%', padding: '10px 14px', textAlign: 'left',
                    background: sortBy === opt.value ? 'var(--rzp-blue)' : 'transparent',
                    color: sortBy === opt.value ? '#fff' : 'var(--text-primary)',
                    border: 'none', borderRadius: '8px', cursor: 'pointer', fontSize: '0.9rem',
                    fontFamily: 'var(--font-body)'
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Category Pills */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '1.5rem', overflowX: 'auto', paddingBottom: '4px' }}>
        {CATEGORIES.map(cat => (
          <button
            key={cat}
            className="category-pill"
            onClick={() => setSelectedCategory(cat)}
            style={{
              background: selectedCategory === cat ? 'var(--rzp-blue)' : 'var(--bg-secondary)',
              color: selectedCategory === cat ? '#fff' : 'var(--text-primary)',
              border: selectedCategory === cat ? 'none' : '1px solid var(--border-color)',
              padding: '8px 18px',
              borderRadius: '24px',
              cursor: 'pointer',
              fontSize: '0.9rem',
              fontWeight: 500,
              whiteSpace: 'nowrap',
              transition: 'all 0.2s',
              fontFamily: 'var(--font-body)'
            }}
          >
            {cat !== 'All' && <span style={{ marginRight: '6px' }}>{getCategoryIcon(cat)}</span>}
            {cat}
          </button>
        ))}
      </div>

      {/* Results count */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          Showing <strong style={{ color: 'var(--text-primary)' }}>{filteredProducts.length}</strong> products
          {selectedCategory !== 'All' && <span> in <strong>{selectedCategory}</strong></span>}
        </p>
      </div>

      {/* Product Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
        gap: '20px',
        marginBottom: '3rem'
      }}>
        <>
          {filteredProducts.map((product, index) => {
            const price = parseFloat(product.offers?.price) || 0;
            const rating = parseFloat(product.aggregateRating?.ratingValue) || 0;
            const reviewCount = product.aggregateRating?.reviewCount || 0;
            const productId = product.productID;
            const imageUrl = product.image || `https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400`;

            return (
              <div
                key={productId}
                className="storefront-card"
                onClick={() => setSelectedProduct(product)}
                style={{
                  background: 'var(--bg-secondary)',
                  borderRadius: '16px',
                  border: '1px solid var(--border-color)',
                  overflow: 'hidden',
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'transform 0.25s ease, box-shadow 0.25s ease',
                  cursor: 'pointer',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                }}
                onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.boxShadow = '0 12px 24px rgba(0,0,0,0.1)' }}
                onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.06)' }}
              >
                {/* Product Image */}
                <div style={{ position: 'relative', paddingTop: '80%', background: 'var(--bg-secondary)', overflow: 'hidden' }}>
                  <img
                    src={imageUrl}
                    alt={product.name}
                    loading="lazy"
                    style={{
                      position: 'absolute', top: 0, left: 0, width: '100%', height: '100%',
                      objectFit: 'cover', transition: 'transform 0.5s'
                    }}
                    onError={(e) => { e.target.src = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400' }}
                  />
                  {/* Category badge */}
                  <div style={{
                    position: 'absolute', top: '16px', left: '16px',
                    background: 'var(--bg-secondary)', backdropFilter: 'blur(8px)',
                     color: 'var(--text-primary)', padding: '6px 12px', borderRadius: '20px',
                     fontSize: '0.75rem', fontWeight: 700, boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                  }}>
                    {getCategoryIcon(product.category)} {product.category}
                  </div>
                  {/* Wishlist button */}
                  <button
                    onClick={(e) => { e.stopPropagation(); toggleWishlist(productId) }}
                    style={{
                      position: 'absolute', top: '16px', right: '16px',
                      background: 'var(--bg-secondary)', backdropFilter: 'blur(8px)',
                       border: 'none', borderRadius: '50%', width: '36px', height: '36px',
                       display: 'flex', alignItems: 'center', justifyContent: 'center',
                       cursor: 'pointer', transition: 'all 0.2s', boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                    }}
                  >
                    <Heart size={16} fill={wishlist.has(productId) ? '#e74c3c' : 'none'} color={wishlist.has(productId) ? '#e74c3c' : '#4a5568'} />
                  </button>
                </div>

                {/* Product Info */}
                <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column' }}>
                  <h3 style={{
                    fontSize: '1.05rem', fontWeight: 800,
                    color: 'var(--text-primary)', marginBottom: '8px',
                    display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
                    lineHeight: '1.3', minHeight: '2.6em'
                  }}>
                    {product.name}
                  </h3>

                  {/* Rating */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
                    <div style={{
                      display: 'flex', alignItems: 'center', gap: '4px',
                      background: rating >= 4 ? '#eafaf1' : rating >= 3 ? '#fff8e1' : '#fdecea',
                      color: rating >= 4 ? '#2ecc71' : rating >= 3 ? '#f59e0b' : '#e74c3c',
                      padding: '4px 10px', borderRadius: '8px', fontSize: '0.85rem', fontWeight: 700
                    }}>
                      <Star size={14} fill="currentColor" /> {rating.toFixed(1)}
                    </div>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 500 }}>
                      ({reviewCount.toLocaleString()} reviews)
                    </span>
                  </div>

                  {/* Price & Actions */}
                  <div style={{ marginTop: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ fontSize: '1.4rem', fontWeight: 900, color: 'var(--rzp-navy)' }}>
                      {formatPrice(price)}
                    </div>

                    <button
                      className="btn btn-primary"
                      onClick={(e) => { e.stopPropagation(); handleAddToCart(productId) }}
                      style={{ padding: '10px 16px', borderRadius: '12px' }}
                    >
                      <ShoppingCart size={16} />
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </>
      </div>

      {/* Product Details Modal */}
      <AnimatePresence>
        {selectedProduct && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setSelectedProduct(null)}
              style={{
                position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                background: 'rgba(7, 38, 84, 0.4)', backdropFilter: 'blur(12px)',
                zIndex: 999
              }}
            />
            <motion.div
              initial={{ opacity: 0, x: '-50%', y: '-40%', scale: 0.95 }}
              animate={{ opacity: 1, x: '-50%', y: '-50%', scale: 1 }}
              exit={{ opacity: 0, x: '-50%', y: '-40%', scale: 0.95 }}
              style={{
                position: 'fixed', top: '50%', left: '50%',
                width: '90%', maxWidth: '900px', maxHeight: '90vh', overflowY: 'auto',
                background: 'var(--bg-primary)', borderRadius: '24px', zIndex: 1000,
                 boxShadow: '0 24px 64px rgba(0,0,0,0.2)', display: 'flex', flexDirection: 'column'
              }}
            >
              <button 
                onClick={() => setSelectedProduct(null)}
                style={{ position: 'absolute', top: '20px', right: '20px', background: 'var(--bg-secondary)', border: 'none', borderRadius: '50%', width: '40px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', zIndex: 10 }}
              >
                <X size={20} color="var(--text-muted)" />
              </button>
              
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '32px', padding: '32px' }}>
                {/* Image Section */}
                <div style={{ flex: '1 1 350px', background: 'var(--bg-secondary)', borderRadius: '16px', overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px' }}>
                  <img src={selectedProduct.image || `https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400`} alt={selectedProduct.name} style={{ width: '100%', objectFit: 'contain', borderRadius: '12px' }} />
                </div>
                
                {/* Details Section */}
                <div style={{ flex: '1 1 350px', display: 'flex', flexDirection: 'column' }}>
                  <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                    <span style={{ background: 'var(--rzp-blue-light)', color: 'var(--rzp-blue)', padding: '4px 12px', borderRadius: '20px', fontSize: '0.8rem', fontWeight: 700 }}>
                      {selectedProduct.category}
                    </span>
                    {selectedProduct.offers?.availability === 'InStock' && (
                      <span style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)', padding: '4px 12px', borderRadius: '20px', fontSize: '0.8rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Check size={14} /> In Stock ({selectedProduct.offers?.inventoryLevel})
                      </span>
                    )}
                  </div>
                  
                  <h2 style={{ fontSize: '2rem', fontWeight: 900, color: 'var(--rzp-navy)', marginBottom: '16px', lineHeight: '1.2' }}>
                    {selectedProduct.name}
                  </h2>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#f59e0b' }}>
                      {[...Array(5)].map((_, i) => (
                        <Star key={i} size={18} fill={i < Math.floor(selectedProduct.aggregateRating?.ratingValue || 0) ? 'currentColor' : 'none'} />
                      ))}
                    </div>
                    <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                      {parseFloat(selectedProduct.aggregateRating?.ratingValue || 0).toFixed(1)} / 5.0 ({selectedProduct.aggregateRating?.reviewCount} reviews)
                    </span>
                  </div>
                  
                  <div style={{ fontSize: '2.5rem', fontWeight: 900, color: 'var(--text-primary)', marginBottom: '24px' }}>
                    {formatPrice(parseFloat(selectedProduct.offers?.price || 0))}
                  </div>
                  
                  <p style={{ fontSize: '1.05rem', color: 'var(--text-secondary)', lineHeight: '1.7', marginBottom: '32px' }}>
                    {selectedProduct.description}
                  </p>
                  
                  {/* Tags */}
                  {selectedProduct.keywords && (
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '32px' }}>
                      {selectedProduct.keywords.split(',').map(tag => (
                        <span key={tag} style={{ background: 'var(--bg-secondary)', color: 'var(--text-muted)', padding: '6px 14px', borderRadius: '8px', fontSize: '0.85rem', fontWeight: 500 }}>
                          #{tag.trim()}
                        </span>
                      ))}
                    </div>
                  )}
                  
                  <div style={{ display: 'flex', gap: '16px', marginTop: 'auto' }}>
                    <button
                      className="btn btn-primary"
                      onClick={() => handleAddToCart(selectedProduct.productID)}
                      style={{ flex: 2, padding: '16px', borderRadius: '16px', fontSize: '1.05rem', display: 'flex', justifyContent: 'center' }}
                    >
                      <ShoppingCart size={20} /> Add to Cart
                    </button>
                    <button
                      className="btn"
                      onClick={() => { setSelectedProduct(null); handleAskAI(selectedProduct.name); }}
                      style={{ flex: 1, padding: '16px', borderRadius: '16px', background: 'var(--rzp-blue-light)', color: 'var(--rzp-blue)', fontSize: '1.05rem', display: 'flex', justifyContent: 'center' }}
                    >
                      <Sparkles size={20} /> Ask AI
                    </button>
                  </div>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  )
}
