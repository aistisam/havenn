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

package haven;

import java.util.*;
import java.awt.Color;
import haven.render.*;
import haven.render.sl.*;

public class Glob {
	public final OCache oc = new OCache(this);
	public final MCache map;
	public final Session sess;
	public final Loader loader = new Loader();
	public double gtime, sgtime, epoch = Utils.rtime();
	public Astronomy ast;
	public Party party;
	public Color lightamb = new Color(0.6f, 0.6f, 0.4f);
	public Color lightdif = new Color(0.7f, 0.7f, 0.6f);
	public Color lightspc = new Color(0.7f, 0.7f, 0.6f);
	public double lightang = 0.5, lightelev = 0.5;
	public double lchange = -1;
	public Indir <Resource> sky1 = null, sky2 = null;
	public double skyblend = 0.0;
	private final Map <String, CAttr> cattr     = new HashMap <String, CAttr>();
	private Map <Indir <Resource>, Object> wmap = new HashMap <Indir <Resource>, Object>();

	public Glob(Session sess) {
		this.sess = sess;
		map       = new MCache(sess);
		party     = new Party(this);
	}

	@Resource.PublishedCode(name = "wtr")
	public static interface Weather {
		public Pipe.Op state();
		public void update(Object... args);
		public boolean tick(double dt);
	}

	public static class CAttr {
		public final String nm;
		public int base, comp;

		public CAttr(String nm, int base, int comp) {
			this.nm   = nm.intern();
			this.base = base;
			this.comp = comp;
		}

		public void update(int base, int comp) {
			if ((base == this.base) && (comp == this.comp)) {
				return;
			}
			this.base = base;
			this.comp = comp;
		}
	}

	private double lastctick = 0;
	public void ctick() {
		double now = Utils.rtime();
		double dt;

		if (lastctick == 0) {
			dt = 0;
		}else {
			dt = Math.max(now - lastctick, 0.0);
		}

		tickgtime(now, dt);
		oc.ctick(dt);
		map.ctick(dt);

		lastctick = now;
	}

	public void gtick(Render g) {
		oc.gtick(g);
		map.gtick(g);
	}

	private static final double itimefac = 3.0;
	private double stimefac = itimefac, ctimefac = itimefac;
	private void tickgtime(double now, double dt) {
		double sgtime = this.sgtime + ((now - epoch) * stimefac);

		gtime += dt * ctimefac;
		if ((sgtime > gtime) && (ctimefac / stimefac < 1.1)) {
			ctimefac += Math.min((sgtime - gtime) * 0.001, 0.02) * dt;
		}else if ((sgtime < gtime) && (stimefac / ctimefac < 1.1)) {
			ctimefac -= Math.min((gtime - sgtime) * 0.001, 0.02) * dt;
		}
		ctimefac += Math.signum(stimefac - ctimefac) * 0.002 * dt;
	}

	private void updgtime(double sgtime, boolean inc) {
		double now   = Utils.rtime();
		double delta = now - epoch;

		epoch = now;
		if ((this.sgtime == 0) || !inc || (Math.abs(sgtime - this.sgtime) > 500)) {
			this.gtime = this.sgtime = sgtime;
			return;
		}
		if ((sgtime - this.sgtime) > 1) {
			double utimefac = (sgtime - this.sgtime) / delta;
			double f        = Math.min(delta * 0.01, 0.5);
			stimefac = (stimefac * (1 - f)) + (utimefac * f);
		}
		this.sgtime = sgtime;
	}

	public String gtimestats() {
		double sgtime = this.sgtime + ((Utils.rtime() - epoch) * stimefac);

		return(String.format("%.2f %.2f %.2f %.2f %.2f %.2f %.2f", gtime, this.sgtime, epoch, sgtime, sgtime - gtime, ctimefac, stimefac));
	}

	public double globtime() {
		return(gtime);
	}

	public void blob(Message msg) {
		boolean inc = msg.uint8() != 0;

		while (!msg.eom()) {
			String   t = msg.string().intern();
			Object[] a = msg.list();
			int      n = 0;
			if (t == "tm") {
				updgtime(((Number)a[n++]).doubleValue(), inc);
			} else if (t == "astro") {
				double  dt    = ((Number)a[n++]).doubleValue();
				double  mp    = ((Number)a[n++]).doubleValue();
				double  yt    = ((Number)a[n++]).doubleValue();
				boolean night = (Integer)a[n++] != 0;
				Color   mc    = (Color)a[n++];
				int     is    = (n < a.length) ? ((Number)a[n++]).intValue() : 1;
				double  sp    = (n < a.length) ? ((Number)a[n++]).doubleValue() : 0.5;
				double  sd    = (n < a.length) ? ((Number)a[n++]).doubleValue() : 0.5;
				double  years = (n < a.length) ? ((Number)a[n++]).doubleValue() : 0.5;
				double  ym    = (n < a.length) ? ((Number)a[n++]).doubleValue() : 0.5;
				double  md    = (n < a.length) ? ((Number)a[n++]).doubleValue() : 0.5;
				ast = new Astronomy(dt, mp, yt, night, mc, is, sp, sd, years, ym, md);
			}
		}
	}

	public Collection <Weather> weather() {
		synchronized (this) {
			ArrayList <Weather> ret = new ArrayList <>(wmap.size());
			for (Map.Entry <Indir <Resource>, Object> cur : wmap.entrySet()) {
				Object val = cur.getValue();
				if (val instanceof Weather) {
					ret.add((Weather)val);
				}else {
					try {
						Class <? extends Weather> cl = cur.getKey().get().flayer(Resource.CodeEntry.class).getcl(Weather.class);
						Weather w = Utils.construct(cl.getConstructor(Object[].class), new Object[] { val });
						cur.setValue(w);
						ret.add(w);
					} catch (Loading l) {
					} catch (NoSuchMethodException e) {
						throw(new RuntimeException(e));
					}
				}
			}
			return(ret);
		}
	}

	/* XXX: This is actually quite ugly and there should be a better
	 * way, but until I can think of such a way, have this as a known
	 * entry-point to be forwards-compatible with compiled
	 * resources. */
	public static DirLight amblight(Pipe st) {
		return(((MapView)((PView.WidgetContext)st.get(RenderContext.slot)).widget()).amblight);
	}

	public CAttr getcattr(String nm) {
		synchronized (cattr) {
			CAttr a = cattr.get(nm);
			if (a == null) {
				a = new CAttr(nm, 0, 0);
				cattr.put(nm, a);
			}
			return(a);
		}
	}

	public void cattr(String nm, int base, int comp) {
		synchronized (cattr) {
			CAttr a = cattr.get(nm);
			if (a == null) {
				a = new CAttr(nm, base, comp);
				cattr.put(nm, a);
			}else {
				a.update(base, comp);
			}
		}
	}

	public static class FrameInfo extends State {
		public static final Slot <FrameInfo> slot = new Slot <>(Slot.Type.SYS, FrameInfo.class);
		public static final Uniform u_globtime    = new Uniform(Type.FLOAT, "globtime", p -> {
			FrameInfo inf = p.get(slot);
			return((inf == null) ? 0.0f : (float)(inf.globtime % 10000.0));
		}, slot);
		public final double globtime;

		public FrameInfo(Glob glob) {
			this.globtime = glob.globtime();
		}

		public ShaderMacro shader() {
			return(null);
		}

		public void apply(Pipe p) {
			p.put(slot, this);
		}

		public static Expression globtime() {
			return(u_globtime.ref());
		}

		public String toString() {
			return(String.format("#<globinfo @%fs>", globtime));
		}
	}
}
