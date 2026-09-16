import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/login',
            component: () => import('../views/user/Login.vue'),
            meta: { title: '用户登录' }
        }, {
            path: '/register',
            component: () => import('../views/user/Register.vue'),
            meta: { title: '用户注册' }
        }, {
            path: '/forgot',
            component: () => import('../views/user/ForgotPassword.vue'),
            meta: { title: '找回密码' }
        }, {
            path: '/',
            component: () => import('../views/Home.vue'),
            redirect: '/recommendation',
            children: [
                {
                    path: '/recommendation',
                    name: 'Recommendation',
                    component: () => import('../views/homepage/index.vue'),
                    meta: { title: '书城首页' }
                }, {
                    path: '/search',
                    name: 'Search',
                    component: () => import('../views/SearchResult.vue'),
                    meta: { title: '搜索结果' }
                }, {
                    path: '/author/:name',
                    name: 'AuthorHome',
                    component: () => import('../views/author/AuthorHome.vue'),
                    meta: { title: '作者主页' }
                }, {
                    path: '/dashboard',
                    name: 'Dashboard',
                    component: () => import('../views/user/Dashboard.vue'),
                    meta: { title: '个人主页' }
                }, {
                    path: '/cart',
                    name: 'Cart',
                    component: () => import('../views/Cart/AllBooksInCart.vue'),
                    meta: { title: '购物车' }
                }, {
                    path: '/checkout',
                    name: 'checkout',
                    component: () => import('../views/order/Confirm.vue'),
                    meta: { title: '确认订单' }
                }, {
                    path: '/bookdetail/:bookId',
                    name: 'BookDetail',
                    component: () => import('../views/book/BookDetail.vue'),
                    meta: { title: '图书详情' }
                }, {
                    path: '/order/confirm',
                    name: 'confirmOrder',
                    component: () => import('../views/order/Confirm.vue'),
                    meta: { title: '确认订单' }
                }, {
                    path: '/order/:orderId/payment',
                    name: 'orderPayment',
                    component: () => import('../views/order/Payment.vue'),
                    meta: { title: '支付订单' }
                }, {
                    path: '/order/:orderId/result',
                    name: 'orderResult',
                    component: () => import('../views/order/Result.vue'),
                    meta: { title: '订单结果' }
                }, {
                    path: '/orders',
                    name: 'orders',
                    component: () => import('../views/order/GetAllOrders.vue'),
                    meta: { title: '你的订单' }
                }, {
                    path: '/orders/:orderId',
                    name: 'orderDetail',
                    component: () => import('../views/order/OrderDetail.vue'),
                    meta: { title: '订单详情' }
                }, {
                    path: '/addresses',
                    name: 'addresses',
                    component: () => import('../views/address/GetAllAddresses.vue'),
                    meta: { title: '收货地址' }
                }, {
                    path: '/coupon/allcoupons',
                    name: 'allCoupons',
                    component: () => import('../views/Coupon/AllCoupons.vue'),
                    meta: { title: '所有优惠券' }
                }
            ]
        }, {
            path: '/admin',
            component: () => import('../views/admin/AdminLayout.vue'),
            redirect: '/admin/dashboard',
            meta: { requiresAdmin: true, title: '管理后台' },
            children: [
                {
                    path: 'dashboard',
                    name: 'AdminDashboard',
                    component: () => import('../views/admin/Dashboard.vue'),
                    meta: { requiresAdmin: true, title: '数据看板' }
                }, {
                    path: 'books',
                    name: 'AdminBooks',
                    component: () => import('../views/admin/BookManage.vue'),
                    meta: { requiresAdmin: true, title: '图书管理' }
                }, {
                    path: 'categories',
                    name: 'AdminCategories',
                    component: () => import('../views/admin/CategoryManage.vue'),
                    meta: { requiresAdmin: true, title: '分类管理' }
                }, {
                    path: 'inventory',
                    name: 'AdminInventory',
                    component: () => import('../views/admin/InventoryManage.vue'),
                    meta: { requiresAdmin: true, title: '库存管理' }
                }, {
                    path: 'orders',
                    name: 'AdminOrders',
                    component: () => import('../views/admin/OrderManage.vue'),
                    meta: { requiresAdmin: true, title: '订单管理' }
                }, {
                    path: 'coupons',
                    name: 'AdminCoupons',
                    component: () => import('../views/admin/CouponManage.vue'),
                    meta: { requiresAdmin: true, title: '优惠券管理' }
                }
            ]
        }, {
            path: '/404',
            name: '404',
            component: () => import('../views/NotFound.vue'),
            meta: { title: '404' }
        }, {
            path: '/:catchAll(.*)',
            redirect: '/404'
        }
    ]
})

router.beforeEach((to, _from, next) => {
    const token = sessionStorage.getItem('token')
    const role = sessionStorage.getItem('role')

    if (to.meta.title) {
        document.title = to.meta.title as string
    }

    if (to.meta.requiresAdmin || to.path.startsWith('/admin')) {
        if (!token) {
            next('/login')
        } else if (role !== 'ADMIN') {
            next('/recommendation')
        } else {
            next()
        }
        return
    }

    if (token) {
        next()
    } else {
        if (to.path === '/login' || to.path === '/register' || to.path === '/forgot' || to.path === '/recommendation' || to.path === '/search' || to.path.startsWith('/author/')) {
            next()
        } else {
            next('/login')
        }
    }
})

export default router
export { router }
