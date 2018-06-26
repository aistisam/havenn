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

package haven.render;

import java.util.*;
import haven.*;
import haven.render.sl.*;
import haven.render.State.Slot;

public abstract class States {
    private States() {}

    private abstract static class Builtin extends State {
	public ShaderMacro shader() {return(null);}
    }

    public static abstract class StandAlone extends Builtin {
	public final Slot<StandAlone> slot;

	StandAlone(Slot.Type type) {
	    this.slot = new Slot<StandAlone>(type, StandAlone.class);
	}

	public void apply(Pipe p) {p.put(slot, this);}
    }

    public static final Slot<State> vxf = new Slot<State>(Slot.Type.SYS, State.class);

    public static Slot<Viewport> viewport = new Slot<Viewport>(Slot.Type.SYS, Viewport.class);
    public static class Viewport extends Builtin {
	public final Area area;

	public Viewport(Area area) {
	    this.area = area;
	}

	public boolean equals(Object o) {
	    return((o instanceof Viewport) && (((Viewport)o).area.equals(area)));
	}

	public void apply(Pipe p) {p.put(viewport, this);}
    }

    public static Slot<Scissor> scissor = new Slot<Scissor>(Slot.Type.SYS, Scissor.class);
    public static class Scissor extends Builtin {
	public final Area area;

	public Scissor(Area area) {
	    this.area = area;
	}

	public boolean equals(Object o) {
	    return((o instanceof Scissor) && (((Scissor)o).area.equals(area)));
	}

	public void apply(Pipe p) {p.put(scissor, this);}
    }

    public static final StandAlone depthtest = new StandAlone(Slot.Type.GEOM) {};
    public static final StandAlone maskdepth = new StandAlone(Slot.Type.GEOM) {};

    public static Slot<Blending> blend = new Slot<Blending>(Slot.Type.SYS, Blending.class);
    public static class Blending extends Builtin {
	public final Function cfn, afn;
	public final Factor csrc, cdst, asrc, adst;
	public final FColor color;

	public enum Function {
	    ADD, SUB, RSUB, MIN, MAX;
	}
	public enum Factor {
	    ZERO, ONE,
	    SRC_COLOR, DST_COLOR, INV_SRC_COLOR, INV_DST_COLOR,
	    SRC_ALPHA, DST_ALPHA, INV_SRC_ALPHA, INV_DST_ALPHA,
	    CONST_COLOR, INV_CONST_COLOR, CONST_ALPHA, INV_CONST_ALPHA,
	}

	public Blending(Function cfn, Factor csrc, Factor cdst, Function afn, Factor asrc, Factor adst, FColor color) {
	    this.cfn = cfn; this.csrc = csrc; this.cdst = cdst;
	    this.afn = afn; this.asrc = asrc; this.adst = adst;
	    this.color = color;
	}

	public Blending(Function cfn, Factor csrc, Factor cdst, Function afn, Factor asrc, Factor adst) {
	    this(cfn, csrc, cdst, afn, asrc, adst, null);
	}
	public Blending(Factor csrc, Factor cdst, Factor asrc, Factor adst) {
	    this(Function.ADD, csrc, cdst, Function.ADD, asrc, adst);
	}
	public Blending(Function fn, Factor src, Factor dst) {
	    this(fn, src, dst, fn, src, dst);
	}
	public Blending(Factor src, Factor dst) {
	    this(Function.ADD, src, dst);
	}
	public Blending() {
	    this(Factor.SRC_ALPHA, Factor.INV_SRC_ALPHA);
	}

	public int hashCode() {
	    return(Objects.hash(cfn, csrc, cdst, afn, asrc, adst, color));
	}

	public boolean equals(Object o) {
	    if(!(o instanceof Blending))
		return(false);
	    Blending that = (Blending)o;
	    return((this.cfn == that.cfn) && (this.csrc == that.csrc) && (this.cdst == that.cdst) &&
		   (this.afn == that.afn) && (this.asrc == that.asrc) && (this.adst == that.adst) &&
		   Utils.eq(this.color, that.color));
	}

	public void apply(Pipe p) {p.put(blend, this);}
    }

    public static Slot<LineWidth> linewidth = new Slot<LineWidth>(Slot.Type.GEOM, LineWidth.class);
    public static class LineWidth extends Builtin {
	public final float w;

	public LineWidth(float w) {
	    this.w = w;
	}
	public LineWidth(double w) {this((float)w);}
	public LineWidth(int w) {this((float)w);}

	public boolean equals(Object o) {
	    return((o instanceof LineWidth) && (((LineWidth)o).w == this.w));
	}

	public void apply(Pipe p) {p.put(linewidth, this);}
    }
}