import { type NextRequest, NextResponse } from "next/server";

const BACKEND = process.env.BACKEND_URL ?? "http://localhost:8080";

async function proxy(req: NextRequest, params: Promise<{ path: string[] }>) {
  const { path } = await params;
  const url = `${BACKEND}/api/${path.join("/")}${req.nextUrl.search}`;

  const headers = new Headers();
  req.headers.forEach((v, k) => {
    if (!["host", "connection"].includes(k)) headers.set(k, v);
  });

  const upstream = await fetch(url, {
    method: req.method,
    headers,
    body: req.method !== "GET" && req.method !== "HEAD" ? req.body : undefined,
    // @ts-expect-error – Node fetch duplex requirement
    duplex: "half",
  });

  const resHeaders = new Headers();
  upstream.headers.forEach((v, k) => resHeaders.set(k, v));

  return new NextResponse(upstream.body, {
    status: upstream.status,
    headers: resHeaders,
  });
}

export const GET = (req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) =>
  proxy(req, ctx.params);
export const POST = (req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) =>
  proxy(req, ctx.params);
export const PUT = (req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) =>
  proxy(req, ctx.params);
export const DELETE = (req: NextRequest, ctx: { params: Promise<{ path: string[] }> }) =>
  proxy(req, ctx.params);
