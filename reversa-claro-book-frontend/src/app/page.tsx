"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useAuth } from "src/app/contexts/AuthContext";

export default function Home() {
  const { user, loading } = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (loading) return;
    if (!user) router.replace("/login");
    else router.replace(user.role === "ADMIN" ? "/dashboard" : "/book");
  }, [user, loading, router]);
  return null;
}
