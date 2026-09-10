package com.inventory.common.tenant;

import java.util.Collection;
import java.util.Optional;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.inventory.common.model.TenantAwareEntity;

import jakarta.persistence.EntityNotFoundException;

/**
 * Defense-in-depth: reject tenant-aware entities loaded outside the active shop.
 * Covers bare {@code findById} / query results that omit an explicit businessId predicate.
 */
@Aspect
@Component
public class TenantAccessAspect {

    @AfterReturning(
        pointcut = "execution(* com.inventory..repository..*(..))",
        returning = "result"
    )
    public void guardTenantResult(Object result) {
        if (result == null) {
            return;
        }
        if (result instanceof Optional<?> optional) {
            optional.ifPresent(this::assertAllowed);
            return;
        }
        if (result instanceof Collection<?> collection) {
            collection.forEach(this::assertAllowed);
            return;
        }
        assertAllowed(result);
    }

    private void assertAllowed(Object entity) {
        if (!(entity instanceof TenantAwareEntity tenantEntity)) {
            return;
        }
        TenantContext.getBusinessId().ifPresent(current -> {
            if (tenantEntity.getBusinessId() != null && !current.equals(tenantEntity.getBusinessId())) {
                throw new EntityNotFoundException("Entity not found for the current business");
            }
        });
    }
}
