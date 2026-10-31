package com.tokoku.demos.web.d171;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Spring Cache dengan Caffeine (spring.cache.* di application.yml). Profil refresh-no-loader: lihat D17.1. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
class CacheConfig {

}
