package me.cortex.voxy.client.core.model.bakery;


import com.mojang.blaze3d.vertex.VertexConsumer;
import me.cortex.voxy.client.core.model.BakeLayer;
import me.cortex.voxy.common.util.MemoryBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.lwjgl.system.MemoryUtil;

// 1.20.1 backport: Uses 1.20.1 BakedQuad API (getVertices() int array).
public final class ReuseVertexConsumer implements VertexConsumer {
    public static final int VERTEX_FORMAT_SIZE = 24;
    private MemoryBuffer buffer = new MemoryBuffer(8192);
    private long ptr;
    private int count;
    private int defaultMeta;

    public boolean anyShaded;
    public boolean anyDarkendTex;
    public boolean anyDiscard;

    private final int globalOrMetadata;
    public ReuseVertexConsumer() {
        this(0);
    }
    public ReuseVertexConsumer(int globalOrMetadata) {
        this.reset();
        this.globalOrMetadata = globalOrMetadata;
    }

    public ReuseVertexConsumer setDefaultMeta(int meta) {
        this.defaultMeta = meta;
        return this;
    }

    public int getDefaultMeta() {
        return this.defaultMeta;
    }

    @Override
    public ReuseVertexConsumer vertex(double x, double y, double z) {
        this.ensureCanPut();
        this.ptr += VERTEX_FORMAT_SIZE; this.count++; //Goto next vertex
        this.meta(this.defaultMeta|this.globalOrMetadata);
        MemoryUtil.memPutFloat(this.ptr, (float) x);
        MemoryUtil.memPutFloat(this.ptr + 4, (float) y);
        MemoryUtil.memPutFloat(this.ptr + 8, (float) z);
        return this;
    }

    public ReuseVertexConsumer meta(int metadata) {
        this.anyDiscard |= (metadata&1)!=0;
        MemoryUtil.memPutInt(this.ptr + 12, metadata);
        return this;
    }

    public ReuseVertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this;
    }

    public VertexConsumer setColor(int i) {
        return this;
    }

    public ReuseVertexConsumer setUv(float u, float v) {
        MemoryUtil.memPutFloat(this.ptr + 16, u);
        MemoryUtil.memPutFloat(this.ptr + 20, v);
        return this;
    }

    public ReuseVertexConsumer setUv1(int u, int v) {
        return this;
    }

    public ReuseVertexConsumer setUv2(int u, int v) {
        return this;
    }

    public ReuseVertexConsumer setNormal(float x, float y, float z) {
        return this;
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        // 1.20.1: required by VertexConsumer
    }

    public void endVertex() {
        // 1.20.1: required by VertexConsumer
    }

    public VertexConsumer uv(float u, float v) {
        return this;
    }

    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    public VertexConsumer uv2(int u, int v) {
        return this;
    }

    public VertexConsumer normal(float x, float y, float z) {
        // 1.20.1: required by VertexConsumer
        return this;
    }

    public void unsetDefaultColor() {
        // 1.20.1: required by VertexConsumer interface
    }

    public VertexConsumer setLineWidth(float f) {
        return null;
    }

    public ReuseVertexConsumer quad(BakedQuad quad, BakeLayer layer) {
        return this.quad(quad, false, layer);
    }

    public ReuseVertexConsumer quad(BakedQuad quad, boolean forceSolid, BakeLayer layer) {
        int meta = 0;
        meta |= forceSolid?0:(layer!=BakeLayer.SOLID?1:0);//has discard
        meta |= (quad.getTintIndex() != -1)?4:0;//has tinting
        return this.quad(quad, meta);
    }

    public ReuseVertexConsumer quad(BakedQuad quad, int metadata) {
        this.anyShaded |= quad.hasAmbientOcclusion();
        // 1.20.1: MipmapStrategy doesn't exist; skip dark cutout detection
        this.ensureCanPut();
        int[] vertices = quad.getVertices();
        // 1.20.1 vertex format: 8 ints per vertex (x,y,z at 0-2, uv at 4-5 as float bits)
        for (int i = 0; i < 4; i++) {
            int base = i * 8;
            float x = Float.intBitsToFloat(vertices[base + 0]);
            float y = Float.intBitsToFloat(vertices[base + 1]);
            float z = Float.intBitsToFloat(vertices[base + 2]);
            this.vertex(x, y, z);
            float u = Float.intBitsToFloat(vertices[base + 4]);
            float v = Float.intBitsToFloat(vertices[base + 5]);
            this.setUv(u, v);

            this.meta(metadata|this.globalOrMetadata);
        }
        return this;
    }

    private void ensureCanPut() {
        if ((long) (this.count + 5) * VERTEX_FORMAT_SIZE < this.buffer.size) {
            return;
        }
        long offset = this.ptr-this.buffer.address;
        //1.5x the size
        var newBuffer = new MemoryBuffer((((int)(this.buffer.size*2)+VERTEX_FORMAT_SIZE-1)/VERTEX_FORMAT_SIZE)*VERTEX_FORMAT_SIZE);
        this.buffer.cpyTo(newBuffer.address);
        this.buffer.free();
        this.buffer = newBuffer;
        this.ptr = offset + newBuffer.address;
    }

    public ReuseVertexConsumer reset() {
        this.anyShaded = false;
        this.anyDarkendTex = false;
        this.anyDiscard = false;
        this.defaultMeta = 0;//RESET THE DEFAULT META
        this.count = 0;
        this.ptr = this.buffer.address - VERTEX_FORMAT_SIZE;//the thing is first time this gets incremented by FORMAT_STRIDE
        return this;
    }

    public void free() {
        this.ptr = 0;
        this.count = 0;
        this.buffer.free();
        this.buffer = null;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public int quadCount() {
        if (this.count%4 != 0) throw new IllegalStateException();
        return this.count/4;
    }

    public long getAddress() {
        return this.buffer.address;
    }
}
