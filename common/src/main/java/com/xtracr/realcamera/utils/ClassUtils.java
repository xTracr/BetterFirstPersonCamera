package com.xtracr.realcamera.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import javax.annotation.Nullable;

public class ClassUtils {
    
	/**
	 * 
	 * You'd better use:
	 * <p>Fabric: {@code FabricLoader.getInstance().isModLoaded(modid)}
	 * <p>Forge: {@code ModList.get().isLoaded(modid)}
	 * 
	 */
	public static boolean isLoaded(final String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException exception) {
			return false;
		}
	}

	public static boolean isLoaded(final String className, final String... classNames) {
		boolean ret = isLoaded(className);
		for (final String name : classNames) {
			ret = ret || isLoaded(name);
		}
		return ret;
	}

    public static Optional<Class<?>> getClass(final String className) {
        try {
			return Optional.of(Class.forName(className));
		} catch (ClassNotFoundException exception) {
			return Optional.empty();
		}
    }

    public static Optional<Class<?>> getClass(final String className, final String... classNames) {
		Optional<Class<?>> ret = getClass(className);
		for (final String name : classNames) {
			if (ret.isPresent()) return ret;
			ret = getClass(name);
		}
		return ret;
    }

	public static Optional<Field> getField(final Optional<Class<?>> classObj, final String fieldName) {
		return classObj.map(cls -> {
			try {
				final Field fld = cls.getField(fieldName);
				fld.setAccessible(true);
				return fld;
			} catch (NoSuchFieldException | SecurityException exception) {
				return null;
			}
		});
	}
	
	public static Optional<Field> getDeclaredField(final Optional<Class<?>> classObj, final String fieldName) {
		return classObj.map(cls -> {
			try {
				final Field fld = cls.getDeclaredField(fieldName);
				fld.setAccessible(true);
				return fld;
			} catch (NoSuchFieldException | SecurityException exception) {
				return null;
			}
		});
	}

    public static Optional<?> getFieldValue(final Optional<Field> field, @Nullable final Object object) {
        return field.map(fld -> {
            try {
                return fld.get(object);
            } catch (IllegalArgumentException | IllegalAccessException exception) {
                return null;
            }
        });
    }

    public static void setField(final Optional<Field> field, @Nullable final Object object, Object value) {
        field.ifPresent(fld -> {
            try {
                fld.set(object, value);
            } catch (IllegalArgumentException | IllegalAccessException exception) {
                
            }
        });
    }

    public static Optional<Method> getMethod(final Optional<Class<?>> classObj, final String methodName, Class<?>... args) {
		return classObj.map(cls -> {
			try {
				final Method mtd = cls.getMethod(methodName, args);
				mtd.setAccessible(true);
				return mtd;
			} catch (NoSuchMethodException | SecurityException exception) {
			    return null;
			}
		});
	}
	
    public static Optional<Method> getDeclaredMethod(final Optional<Class<?>> classObj, final String methodName, Class<?>... args) {
		return classObj.map(cls -> {
			try {
				final Method mtd = cls.getDeclaredMethod(methodName, args);
				mtd.setAccessible(true);
				return mtd;
			} catch (NoSuchMethodException | SecurityException exception) {
			    return null;
			}
		});
	}
	
}
