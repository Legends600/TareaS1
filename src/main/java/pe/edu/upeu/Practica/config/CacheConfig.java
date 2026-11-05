package pe.edu.upeu.Practica.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/*
 * Activa la cache y define que pasa si Redis cae: se registra una
 * advertencia y la peticion sigue respondiendo desde Oracle.
 *
 * order: la cache envuelve a @Transactional (cuyo orden es LOWEST_PRECEDENCE).
 * Asi @CacheEvict limpia despues del commit (nadie vuelve a guardar el dato
 * viejo entre la limpieza y el commit) y un acierto no abre transaccion.
 */
@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache {} no disponible al leer {} | {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key,
                                            Object value) {
                log.warn("Cache {} no disponible al guardar {} | {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache {} no disponible al invalidar {} | {}",
                        cache.getName(), key, ex.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Cache {} no disponible al limpiar | {}",
                        cache.getName(), ex.getMessage());
            }
        };
    }
}
