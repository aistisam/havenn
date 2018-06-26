/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven.render.gl;

import java.util.*;
import javax.media.opengl.*;
import haven.*;
import haven.render.*;
import haven.render.sl.*;

public interface UniformApplier<T> {
    public static class TypeMapping {
	private static final Map<Type, TypeMapping> mappings = new HashMap<>();
	private final Map<Class<?>, UniformApplier<?>> reg = new HashMap<>();
	private final Map<Class<?>, UniformApplier<?>> cache = new HashMap<>();

	public static <T> void register(Type type, Class<T> cl, UniformApplier<T> fn) {
	    TypeMapping map;
	    synchronized(mappings) {
		map = mappings.computeIfAbsent(type, t -> new TypeMapping());
	    }
	    synchronized(map) {
		map.reg.put(cl, fn);
		map.cache.clear();
	    }
	}

	@SuppressWarnings("unchecked")
	public static <T> UniformApplier<T> get(Type type, Class<T> cl) {
	    /* XXX: *Should* be synchronized, but doesn't actually
	     * need to be. Test and see if there are performance
	     * consequences. */
	    TypeMapping map = mappings.get(type);
	    if(map == null)
		return(null);
	    UniformApplier<T> fn = (UniformApplier<T>)map.cache.get(cl);
	    if(fn == null) {
		for(Map.Entry<Class<?>, UniformApplier<?>> reg : map.reg.entrySet()) {
		    if(reg.getKey().isAssignableFrom(cl)) {
			fn = (UniformApplier<T>)reg.getValue();
			break;
		    }
		}
		map.cache.put(cl, fn);
	    }
	    return(fn);
	}

	@SuppressWarnings("unchecked")
	private static <T> void apply0(BGL gl, UniformApplier<T> fn, GLProgram prog, Uniform var, Object val) {
	    fn.apply(gl, prog, var, (T)val);
	}
	public static void apply(BGL gl, GLProgram prog, Uniform var, Object val) {
	    UniformApplier<?> fn = UniformApplier.TypeMapping.get(var.type, val.getClass());
	    apply0(gl, fn, prog, var, val);
	}

	static {
	    TypeMapping.register(Type.FLOAT, Float.class, (gl, prog, var, n) -> {
		    gl.glUniform1f(prog.uniform(var), n);
		});

	    TypeMapping.register(Type.VEC2, float[].class, (gl, prog, var, a) -> {
		    gl.glUniform2f(prog.uniform(var), a[0], a[1]);
		});
	    TypeMapping.register(Type.VEC2, Coord.class, (gl, prog, var, c) -> {
		    gl.glUniform2f(prog.uniform(var), c.x, c.y);
		});
	    TypeMapping.register(Type.VEC2, Coord3f.class, (gl, prog, var, c) -> {
		    gl.glUniform2f(prog.uniform(var), c.x, c.y);
		});

	    TypeMapping.register(Type.VEC3, float[].class, (gl, prog, var, a) -> {
		    gl.glUniform3f(prog.uniform(var), a[0], a[1], a[2]);
		});
	    TypeMapping.register(Type.VEC3, Coord3f.class, (gl, prog, var, c) -> {
		    gl.glUniform3f(prog.uniform(var), c.x, c.y, c.z);
		});
	    TypeMapping.register(Type.VEC3, java.awt.Color.class, (gl, prog, var, col) -> {
		    gl.glUniform3f(prog.uniform(var), col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);
		});
	    TypeMapping.register(Type.VEC3, FColor.class, (gl, prog, var, col) -> {
		    gl.glUniform3f(prog.uniform(var), col.r, col.g, col.b);
		});

	    TypeMapping.register(Type.VEC4, float[].class, (gl, prog, var, a) -> {
		    gl.glUniform4f(prog.uniform(var), a[0], a[1], a[2], a[3]);
		});
	    TypeMapping.register(Type.VEC4, Coord3f.class, (gl, prog, var, c) -> {
		    gl.glUniform4f(prog.uniform(var), c.x, c.y, c.z, 1);
		});
	    TypeMapping.register(Type.VEC4, java.awt.Color.class, (gl, prog, var, col) -> {
		    gl.glUniform4f(prog.uniform(var), col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f, col.getAlpha() / 255f);
		});
	    TypeMapping.register(Type.VEC4, FColor.class, (gl, prog, var, col) -> {
		    gl.glUniform4f(prog.uniform(var), col.r, col.g, col.b, col.a);
		});

	    TypeMapping.register(Type.SAMPLER2D, GLTexture.Tex2D.class, (gl, prog, var, smp) -> {
		    gl.glActiveTexture(GL.GL_TEXTURE0 + prog.samplerids.get(var));
		    smp.bind(gl);
		});
	}
    }

    public void apply(BGL gl, GLProgram prog, Uniform var, T value);
}